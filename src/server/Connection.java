package server;

import protocol.Error;
import protocol.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.*;

public class Connection extends Thread {
    private final Logger logger;
    private final Socket socket;
    private final LocalDateTime connectionTime;
    private final DateTimeFormatter dtFormatter;
    private String username = null;
    private PrintWriter out = null;

    boolean keepAlive = true;


    /**
     * Wrapper around Socket that handles math processing and our custom protocol
     *
     * @param socket Socket for incoming/outgoing messages
     * @param logger Parent logger to record activity to
     * @throws IOException When the client unexpectedly disconnects
     */
    public Connection(Socket socket, Logger logger) throws IOException {
        this.socket = socket;
        this.connectionTime = LocalDateTime.now();
        this.logger = logger;
        this.dtFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        logger.info("Client with IP %s connected at %s".formatted(socket.getInetAddress(), dtFormatter.format(connectionTime)));
    }


    /**
     * Function that calculates the answer of a math expression
     * <a href="https://stackoverflow.com/a/26227947/6238455">Reference</a>
     * @param str Math expression to be evaluated
     * @return Answer, as a double
     */
    double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)` | number
            //        | functionName `(` expression `)` | functionName factor
            //        | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    if (!eat(')')) throw new RuntimeException("Missing ')'");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (eat('(')) {
                        x = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')' after argument to " + func);
                    } else {
                        x = parseFactor();
                    }
                    x = switch (func) {
                        case "sqrt" -> Math.sqrt(x);
                        case "sin" -> Math.sin(Math.toRadians(x));
                        case "cos" -> Math.cos(Math.toRadians(x));
                        case "tan" -> Math.tan(Math.toRadians(x));
                        default -> throw new RuntimeException("Unknown function: " + func);
                    };
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }

    /**
     * Thin wrapper around eval() that formats the answer to the desired precision.
     * @param request Request to be parsed
     * @return Response with fields filled in
     */
    Response parseExpression(Request request) {
        Response response = new Response();
        try {
            // In order to parse the double, we must force the DecimalFormat class to
            // use the American standard. ¯\_(ツ)_/¯
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            DecimalFormat df = (DecimalFormat)nf;
            df.setMaximumFractionDigits(request.precision);
            df.setMinimumFractionDigits(request.precision);

            // The eval function returns a double.
            double unroundedAnswer = eval(request.expression);
            df.setGroupingUsed(false);
            // ...which is converted to a String by DecimalFormat...
            String answerStr = df.format(unroundedAnswer).replace(",", "");
            logger.fine(unroundedAnswer+" -> "+ answerStr);
            // ...which is finally sent back to a double.
            response.answer = Double.parseDouble(answerStr);

            response.status = Status.SUCCESS;
        } catch (Exception e) {
            response.status = Status.ERROR;
            logger.warning("%s sent invalid expression. Exception: %s".formatted(username, e));
            response.error = Error.INVALID_EXPRESSION;
        }
        return response;
    }

    /**
     * Given a request, perform the desired action.
     * @param request Request to be processed
     */
    void processRequest(Request request) {
        Response response = new Response();
        response.seq = request.seq;

        switch (request.action) {
            case JOIN:
                if (username == null) {
                    response.status = Status.ERROR;
                    response.error = Error.NOT_INTRODUCED;
                } else {
                    response.status = Status.SUCCESS;
                }
                break;
            case LEAVE:
                response.status = Status.QUITTING;
                keepAlive = false;
                break;
            case CALCULATE:
                response = parseExpression(request);
                logger.info("%s sent expression: %s, answer: %f".formatted(username, request.expression, response.answer));
                break;
            default: // Request action was not valid. Send an error response.
                response.error = Error.MALFORMED_REQUEST;
                response.status = Status.ERROR;
        }

        String responseString = """
                [[
                    Seq: %d
                    Status: %s
                    Answer: %s
                    Error: %s
                ]]
                """.formatted(
                response.seq,
                response.status,
                response.answer,
                response.error
        );
//        System.out.println("Sending: "+responseString);
        out.println(responseString);
    }

    /**
     * Wait for incoming network data (Requests) and send responses in an infinite loop.
     */
    @Override
    public void run() {
        BufferedReader in = null;
        try {

            // get the output stream of client
            out = new PrintWriter(
                    socket.getOutputStream(), true);

            // get the inputstream of client
            in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            String requestLine;
            Request request = new Request();

            while ((requestLine = in.readLine()) != null && keepAlive) {
//                System.out.println("Received "+ requestLine);
                String[] parts = requestLine.split(":");
                String tag = parts[0].strip().toLowerCase();

                if (Objects.equals(requestLine.strip(), "[["))
                    tag = "[[";
                else if (Objects.equals(requestLine.strip(), "]]"))
                    tag = "]]";

                if (parts.length !=2 && !tag.contains("[[") && !tag.contains("]]")) {
                    request.action = Action.INVALID;
                    processRequest(request);
                    logger.warning("%s sent malformed request. Offending line was: %s".formatted(username, tag));
                }

                switch (tag) {
                    case "[[" ->
                        // Begin a new request.
                            request = new Request();
                    case "]]" ->
                        // This is the end of a request. Process it.
                            processRequest(request);
                    case "seq" -> request.seq = Integer.parseInt(parts[1].strip());
                    case "username" -> username = parts[1].strip();
                    case "expression" -> request.expression = parts[1].strip();
                    case "precision" -> request.precision = Integer.parseInt(parts[1].strip());
                    case "action" -> {
                        String actionString = parts[1].strip();
                        switch (actionString.toLowerCase()) {
                            case "join" -> request.action = Action.JOIN;
                            case "leave" -> request.action = Action.LEAVE;
                            case "calculate" -> request.action = Action.CALCULATE;
                            default -> request.action = Action.INVALID;
                        }
                    }
                    default -> {
                        logger.severe("%s sent invalid line: %s".formatted(username, requestLine));
                        request.action = Action.INVALID;
                        processRequest(request);
                    }
                }

                if (!keepAlive)
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            LocalDateTime now = LocalDateTime.now();
            long minutes = ChronoUnit.MINUTES.between(connectionTime, now);
            long seconds = ChronoUnit.SECONDS.between(connectionTime, now);
            logger.info("Client '%s' with IP %s disconnected at %s, was connected for %dm%ds".formatted(username, socket.getInetAddress(), dtFormatter.format(connectionTime), minutes, seconds));

            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

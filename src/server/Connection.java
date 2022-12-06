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
import java.util.Date;
import java.util.Locale;
import java.util.logging.*;

public class Connection extends Thread {
    private Socket socket;
    private PrintWriter output;
    private LocalDateTime connectionTime;
    private DateTimeFormatter dtFormatter;
    private String username = null;
    private PrintWriter out = null;

    boolean keepAlive = true;
    boolean loggerIsSetup = false;

    Logger logger = Logger.getLogger("ConnectionLog");
    FileHandler fh;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.connectionTime = LocalDateTime.now();
        this.dtFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");




        logger.info("Client with IP %s connected at %s".formatted(socket.getInetAddress(), dtFormatter.format(connectionTime)));
    }

    void setUpLogger() throws IOException {
        if (loggerIsSetup)
                return;
        DateTimeFormatter logStampFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss");
        fh = new FileHandler("%s_%s.log".formatted(username, logStampFormatter.format(connectionTime)));

        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        loggerIsSetup = true;
    }


    // Thanks, Boann! https://stackoverflow.com/a/26227947/6238455
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
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }

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
            logger.warning("Invalid expression detected. Exception: "+e);
            response.error = Error.INVALID_EXPRESSION;
        }
        return response;
    }

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
                logger.info("Expression: %s, answer: %d".formatted(request.expression, response.answer));
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

                if (requestLine.strip() == "[[")
                    tag = "[[";
                else if (requestLine.strip() == "]]")
                    tag = "]]";

                if (parts.length !=2 && !tag.contains("[[") && !tag.contains("]]")) {
                    request.action = Action.INVALID;
                    processRequest(request);
                    logger.warning("Received malformed request. Offending line was: "+tag);
                }

                switch (tag) {
                    case "[[":
                        // Begin a new request.
                        request = new Request(); break;
                    case "]]":
                        // This is the end of a request. Process it.
                        processRequest(request); break;
                    case "seq":
                        request.seq = Integer.parseInt(parts[1].strip()); break;
                    case "username":
                        username = parts[1].strip();
                        setUpLogger();
                        break;
                    case "expression":
                        request.expression = parts[1].strip(); break;
                    case "precision":
                        request.precision = Integer.parseInt(parts[1].strip()); break;
                    case "action":
                        String actionString = parts[1].strip();
                        switch (actionString.toLowerCase()){
                            case "join":
                                request.action = Action.JOIN; break;
                            case "leave":
                                request.action = Action.LEAVE; break;
                            case "calculate":
                                request.action = Action.CALCULATE; break;
                            default:
                                request.action = Action.INVALID; break;
                        }
                        break;
                    default:
                        logger.severe("Invalid line: "+requestLine);
                        request.action = Action.INVALID;
                        processRequest(request);
                }

                if (keepAlive == false)
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

    private String handleRequest(String request) {
        return "Not Implemented";
    }
}

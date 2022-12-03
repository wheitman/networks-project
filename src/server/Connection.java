package server;

import protocol.Error;
import protocol.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Connection extends Thread {
    private Socket socket;
    private PrintWriter output;
    private LocalDateTime connectionTime;
    private DateTimeFormatter dtFormatter;
    private String username = null;
    private PrintWriter out = null;

    boolean keepAlive = true;

    public Connection(Socket socket) {
        this.socket = socket;
        this.connectionTime = LocalDateTime.now();
        this.dtFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        System.out.println("Client with IP %s connected at %s".formatted(socket.getInetAddress(), dtFormatter.format(connectionTime)));
    }

    double parseExpression(Request request) {
        String expr = request.expression;
        return -1.0;
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
                response.answer = parseExpression(request);
                break;
            default: // Request action was not valid. Send an error response.
                response.error = Error.REQUEST_MALFORMED;
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
        System.out.println("Sending: "+responseString);
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
                System.out.println("Received "+ requestLine);
                String[] parts = requestLine.split(":");
                String tag = parts[0].strip().toLowerCase();

                if (requestLine.strip() == "[[")
                    tag = "[[";
                else if (requestLine.strip() == "]]")
                    tag = "]]";

                if (parts.length !=2 && !tag.contains("[[") && !tag.contains("]]")) {
                    request.action = Action.INVALID;
                    processRequest(request);
                    System.out.println("Tag was: "+tag);
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
                        username = requestLine.split(":")[1].strip(); break;
                    case "action":
                        String actionString = parts[1].strip();
                        switch (actionString.toLowerCase()){
                            case "leave":
                                request.action = Action.LEAVE; break;
                            case "calculate":
                                request.action = Action.CALCULATE; break;
                            default:
                                request.action = Action.INVALID; break;
                        }
                        break;
                    default:
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
            System.out.println("Client '%s' with IP %s disconnected at %s, was connected for %dm%ds".formatted(username, socket.getInetAddress(), dtFormatter.format(connectionTime), minutes, seconds));
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

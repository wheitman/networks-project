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

    void processRequest(Request request) {
        Response response = new Response();
        response.seq = request.seq;

        // If handshake not yet performed, throw error
        if (request.action != Action.JOIN && username == null) {
            response.status = Status.ERROR;
            response.error = Error.NOT_INTRODUCED;
        } else if (request.action == Action.JOIN) {
            response.status = Status.SUCCESS;
        } else if (request.action == Action.LEAVE) {
            response.status = Status.SUCCESS;
            keepAlive = false;
            System.out.println("DIE!");
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

                // Is this the beginning of a new request?
                if (requestLine.contains("[[")) {
                    // Reset the current request string
                    request = new Request();
                } else if (requestLine.contains("]]")) {
                    // This is the end of a request. Process it.
                    processRequest(request);
                } else if (requestLine.contains("Seq")) {
                    request.seq = Integer.parseInt(requestLine.split(":")[1].strip());
                } else if (requestLine.contains("Username")) {
                    username = requestLine.split(":")[1].strip();
                    System.out.println("Welcome, %s".formatted(username));
                } else if (requestLine.contains("Action: leave")) {
                    request.action = Action.LEAVE;
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

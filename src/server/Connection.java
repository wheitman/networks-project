package server;

import protocol.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Connection extends Thread {
    private Socket socket;
    private PrintWriter output;
    private LocalDateTime connectionTime;
    private DateTimeFormatter dtFormatter;
    private String username = null;

    public Connection(Socket socket) {
        this.socket = socket;
        this.connectionTime = LocalDateTime.now();
        this.dtFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        System.out.println("Client with IP %s connected at %s".formatted(socket.getInetAddress(), dtFormatter.format(connectionTime)));
    }

    void processRequest(Request request) {

    }

    @Override
    public void run() {
        PrintWriter out = null;
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

            while ((requestLine = in.readLine()) != null) {

                // Is this the beginning of a new request?
                if (requestLine.contains("[[")) {
                    // Reset the current request string
                    request = new Request();
                } else if (requestLine.contains("]]")) {
                    // This is the end of a request. Process it.
                    processRequest(request);
                }

                // Remove tabs, newlines, and whitespace from the request
                requestLine = requestLine.replaceAll("[\\t ]", "");
                String[] request_parts = requestLine.split(",");
                for (String part : request_parts) {
                    System.out.println(part);
                }
                System.out.printf(
                        " Sent from the client: %s\n",
                        requestLine);
                out.println(requestLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
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

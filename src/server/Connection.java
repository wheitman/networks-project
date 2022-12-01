package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Connection extends Thread {
    private Socket socket;
    private PrintWriter output;

    public Connection(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Create a buffered reader to handle the socket's input stream
            BufferedReader inputStream = new BufferedReader( new InputStreamReader(socket.getInputStream()));
            // Create a PrintWriter to format our output. "True" flushes automatically
            output = new PrintWriter(socket.getOutputStream(), true);
            while (true) {
                String inputStr = inputStream.readLine();
                System.out.println("Received {} from client".formatted(inputStr));
            }

        } catch (Exception e) {
            System.out.println("[CONNECTION] An exception occurred: "+e.getStackTrace());
        }
    }

    private String handleRequest(String request) {
        return "Not Implemented";
    }
}

package client;

import protocol.Action;
import protocol.Request;
import protocol.Response;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Main {

   static PrintWriter out = null;
   static BufferedReader in = null;

    /**
     * Forms a handshake request and sends it to the server
     * @param username The current user
     * @throws IOException whenever the socket closes unexpectedly
     */
    static void sendWelcome(String username) throws IOException {
        Request handshakeReq = new Request();
        handshakeReq.seq = 0;
        handshakeReq.action = Action.JOIN;
        handshakeReq.username = username;

        out.println(handshakeReq.toString());
        String responseLine;
        while ((responseLine = in.readLine()) != null && !(responseLine = in.readLine()).contains("]]")) {
            // Read through the server response to clear the buffer.
        }
    }


    /**
     * @return Response from server, wrapped as a Response object
     * @throws IOException whenever socket closes unexpectedly
     */
    static Response getResponse() throws IOException {
        Response response = new Response();
        String responseLine = in.readLine();

        boolean invalidExpression = false;
        double answer = -1.0;
        while (responseLine != null && !responseLine.contains("]]")) {

            if (responseLine.toLowerCase().contains("answer")) {
                String answerString = responseLine.split(":")[1].strip();
                answer = Double.parseDouble(answerString);

            }

            if (responseLine.toLowerCase().contains("invalid")) {
                invalidExpression = true;
//                System.out.println("Invalid expression. Try again.");
            }
            responseLine = in.readLine();
        }

        if (invalidExpression)
            System.out.println("Invalid expression. Try again.");
        else
            System.out.println("Result: "+answer);
        return response;
    }

    public static void main(String[] args)
    {
        // establish a connection by providing host and port
        // number

        final int port = 1234;
        final String address = "localhost";

        try (Socket socket = new Socket(address, port)) {

            // writing to server
            out = new PrintWriter(
                    socket.getOutputStream(), true);

            // reading from server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // object of scanner class
            Scanner sc = new Scanner(System.in);
            String line = null;

            System.out.println("Connected to %s:%d".formatted(address, port));

            System.out.print("What is your username? ");
            String uname = sc.nextLine();
            System.out.print("How many decimal places would you like your answers to have? ");
            int precision = 3;
            try {
                precision = Integer.parseInt(sc.nextLine());
            } catch (Exception e) {
                System.out.println("Precision should be an integer. Goodbye.");
                return;
            }

            sendWelcome(uname);

            System.out.printf("Welcome, %s. Type a math expression to send to the server, or 'q' to quit.\n", uname);

            String input = "";
            int seq = 1;
            Request req = new Request();
            req.username = uname;
            req.precision = precision;
            req.action = Action.CALCULATE;

            while (true) {
                System.out.print("Expression: ");
                input = sc.nextLine();

                if (input.strip().equalsIgnoreCase("q"))
                    break;

                req.seq = seq;
                req.expression = input;

                out.println(req.toString());

                getResponse();

                seq++;
            }

            System.out.println("Leaving. Goodbye, "+uname);

            req.action = Action.LEAVE;
            out.println(req.toString());

            sc.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

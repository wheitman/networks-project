package client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;

public class Main {

    public static void main(String[] args)
    {
        // establish a connection by providing host and port
        // number
        try (Socket socket = new Socket("localhost", 1234)) {

            // writing to server
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            // reading from server
            BufferedReader in
                    = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));

            // object of scanner class
            Scanner sc = new Scanner(System.in);
            String line = null;

//            System.out.print("What is your username? ");
//            String uname = sc.nextLine();
//            System.out.println("Welcome, %s. Type a math expression to send to the server, or 'q' to quit.".formatted(uname));

            out.println("[[\n" +
                    "   Username: jsmith\n" +
                    "   Seq: 1\n" +
                    "   Action: join\n" +
                    "]]");
            out.flush();

            Thread.sleep(2000);

            out.println("[[\n" +
                    "   Seq: 1\n" +
                    "   Action: leave\n" +
                    "]]");
            out.flush();

            String response;

            while ((response = in.readLine()) != null) {
                System.out.println(response);
            }

//            while (!"exit".equalsIgnoreCase(line)) {
//
//                // reading from user
//                System.out.print("Expression: ");
//                line = sc.nextLine();
//
//                // sending the user input to server
//                out.println(line);
//                out.flush();
//
//                // displaying server reply
//                System.out.println("Response: "
//                        + in.readLine());
//            }

            // closing the scanner object
            sc.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Main {

    public Main() throws IOException {
    }

    public static void main(String[] args) {
        System.out.println("[SERVER] Waiting for client connections...");

        ArrayList<Connection> connections = new ArrayList<>();

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            while (true) {
                // Listen for an incoming connection and accept it
                Socket socket = serverSocket.accept();

                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                String message = (String) ois.readObject();
                System.out.println("Received %s".formatted(message));

                ObjectOutputStream oos = new ObjectOutputStream((socket.getOutputStream()));
                oos.writeObject("I heard: %s".formatted(message));
                ois.close();
                oos.close();
                socket.close();
                if(message.equalsIgnoreCase("exit"))
                    break;
            }
            System.out.println("Shutting down the server");
            serverSocket.close();
        } catch (Exception e) {
            System.out.println("[CONNECTION] An exception occurred: " + e.getStackTrace());
        }
    }
}

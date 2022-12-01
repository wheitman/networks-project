package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Hello from the Client package!");

        InetAddress host = InetAddress.getLocalHost();
        Socket socket = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;

        for (int i=0; i < 5; i++) {
            socket = new Socket(host.getHostName(), 5000);
            oos = new ObjectOutputStream(socket.getOutputStream());
            if (i == 4) {
                oos.writeObject("exit");
            } else {
                oos.writeObject("" + i);
            }

            ois = new ObjectInputStream(socket.getInputStream());
            String msg = (String) ois.readObject();
            System.out.println("Message: %s".formatted(msg));

            ois.close();
            oos.close();
            Thread.sleep(100);
        }
    }
}

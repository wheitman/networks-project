package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.*;

public class Main {


    static Logger logger = Logger.getLogger("ConnectionLog");
    static FileHandler fh;

    public Main() {
    }

    /**
     * Set up logger with custom format to track our activity.
     *
     * @throws IOException whenever log file cannot be created.
     */
    static void setUpLogger() throws IOException {
        DateTimeFormatter logStampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        fh = new FileHandler("%s.log".formatted(logStampFormatter.format(LocalDateTime.now())));
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        });
        logger.addHandler(handler);
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }

    public static void main(String[] args)
    {
        ServerSocket server = null;

        try {

            // Set up server to listen on port 1234.
            server = new ServerSocket(1234);
            server.setReuseAddress(true);

            // Set up a logger to track our activity.
            setUpLogger();


            while (true) {

                // Wait for an incoming connection and assign this to the 'client' Socket.
                Socket client = server.accept();

                // Wrap the socket in a custom Connection class
                Connection clientSock = new Connection(client, logger);

                // Send this Connection to its own thread and restart the loop
                new Thread(clientSock).start();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {

            // Always close the server cleanly, freeing up the port.
            if (server != null) {
                try {
                    server.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

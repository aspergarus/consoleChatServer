package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat server.
 * 
 * @author mustafa1453
 */
public class Server {
    public static final int Port = 8283;
    private List<Connection> connections = 
                    Collections.synchronizedList(new ArrayList<Connection>());
    private ServerSocket server;

    public static void main(String... args) {
        Server srv = new Server();
        srv.run();
    }

    /**
     * Constructor. Create thread for each connection.
     */
    public Server() {
        try {
            server = new ServerSocket(Port);
            System.out.println("Server has been started");
        } catch (IOException e) {
            System.err.println("Could not create server");
        }
    }

    public void run() {
        try {
            while (true) {
                Socket socket = server.accept();

                // Create connection and add to the list.
                Connection con = new Connection(socket);
                connections.add(con);

                // Run thread.
                con.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeAll();
        }
    }

    /**
     * Close all streams and sockets.
     */
    private void closeAll() {
        try {
            server.close();
            connections.stream().forEach((connect) -> {
                connect.close();
            });
        } catch (Exception e) {
            System.err.println("Could not close all streams!");
        }
    }

    /**
     * Class which contains data for current connection.
     * Extends Thread for getting information from user and sent it to other.
     */
    public class Connection extends Thread {
        private BufferedReader in;
        private PrintWriter out;
        private Socket socket;

        private String name = "";

        /**
         * Constructor. Getting username.
         * 
         * @param socket socket of connection.
         */
        public Connection(Socket socket) {
            this.socket = socket;

            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

            } catch (IOException e) {
                System.err.println("Could not create socket");
                close();
            }
        }

        /**
         * Get username from and wait for messages from user. 
         * All received messages resend to other users.
         * 
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                name = in.readLine();
                String users = "users:" + connections
                        .stream()
                        .map((Connection connect) -> connect.name)
                        .collect(Collectors.joining(","));

                // Send to all users, that new user has came and list of all users.
                synchronized(connections) {
                    connections.stream().forEach((Connection connect) -> {
                        connect.out.println(name + " has come");
                        connect.out.println(users);
                    });
                }

                String str = "";
                while (true) {
                    str = in.readLine();
                    if(str.equals("exit")) break;

                    // Send current message to all users.
                    synchronized(connections) {
                        Iterator<Connection> iter = connections.iterator();
                        while(iter.hasNext()) {
                            ((Connection) iter.next()).out.println(name + ": " + str);
                        }
                    }
                }

                // Send to all users, that current user has left.
                synchronized(connections) {
                    connections.stream().forEach((Connection connect) -> {
                        connect.out.println(name + " has left");
                    });
                }
            } catch (IOException e) {
                System.err.println("Could not send data!");
            } finally {
                close();
            }
        }

        /**
         * Close streams and socket.
         */
        public void close() {
            try {
                in.close();
                out.close();
                socket.close();

                /**
                 * Delete connection after closing.
                 */
                connections.remove(this);
            } catch (IOException e) {
                System.err.println("Could not close the streams!");
            }
        }
    }
}
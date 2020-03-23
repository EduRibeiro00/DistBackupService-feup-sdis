package server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;

/**
 * Server class
 */
public class Server {
    private static HashMap<String,String> addressTable; // dns table with pairs <name, ip_address>
    private static int port; // port number
    private static ServerSocket serverSocket; // serverSocket that will be used for communication (TCP!)

    /**
     * Main method
     * @param args args[1] should specify port number
     */
    public static void main(String[] args) {
        // check arguments
        if (args.length < 1) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        // open port
        try {
            port = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException e) {
            System.out.println("Port specified is invalid");
            System.exit(2);
        }

        // create serverSocket
        try {
            serverSocket = new ServerSocket(port);
        }
        catch (IOException e) {
            System.out.println("Could not open datagram serverSocket");
            System.exit(3);
        }

        // create dns table
        addressTable = new HashMap<>();

        // listen to requests
        try {
            processRequests();
        }
        catch (IOException e) {
            System.out.println("Error in communication with client (possible timeout)");
            System.exit(3);
        }

        try {

            serverSocket.close();
        }
        catch (IOException e) {
            System.out.println("Error closing the socket");
        }
    }


    /**
     * Method that loops for ever listening to client requests and responding to them
     * @throws IOException
     */
    private static void processRequests() throws IOException {

        while(true) {
            Socket socket = null;
            System.out.println("Now listening to requests...");

            socket = serverSocket.accept();

            // open streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);




            /*
            String requestString = new String(packet.getData());
            System.out.println("Server: " + requestString.trim());
            String replyString = generateReply(requestString.trim());
            sendReply(replyString, packet);
            byte[] newBuf = new byte[512];
            packet.setData(newBuf);
            */
        }
    }


    /**
     * Main method for processing a request and generating a reply for the client
     * @param requestString - request string from the client
     * @return - the reply string
     */
    private static String generateReply(String requestString) {
        String[] requestArgs = requestString.split(" ");
        if (requestArgs.length < 2) {
            return "-1";
        }

        // checking request type
        switch(requestArgs[0]) {
            case "register": // dns register request
                if (requestArgs.length < 3) {
                    return "-1";
                }
                return processRegisterAndReply(requestArgs[1], requestArgs[2]);

            case "lookup": // dns lookup request
                return processLookupAndReply(requestArgs[1]);

            default:
                return "-1";
        }
    }


    /**
     * Method that processes and replies to a "register" request
     * @param dnsName - the dns name
     * @param ipAddress - the ip address
     * @return - the reply string
     */
    private static String processRegisterAndReply(String dnsName, String ipAddress) {
        addressTable.put(dnsName, ipAddress);
        return String.valueOf(addressTable.size());
    }


    /**
     * Method that processes and replies to a "lookup" request
     * @param dnsName - the dns name
     * @return - the reply string
     */
    private static String processLookupAndReply(String dnsName) {
        String ipAddress = addressTable.get(dnsName);
        return ipAddress == null ? "No entry" : (dnsName + " -> " + ipAddress);
    }


    /**
     * Method that sends the reply to the client
     * @param replyString - reply string to be sent to the client
     * @param packet - packet to send to the client
     */
    private static void sendReply(String replyString, DatagramPacket packet) throws IOException {
        packet.setData(replyString.getBytes());
        serverSocket.send(packet);
    }
}
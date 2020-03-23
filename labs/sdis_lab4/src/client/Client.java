package client;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

/**
 * Client class
 */
public class Client {
    private static int TIMEOUT = 10000; // socket operation timeout (10 seconds)
    private static String host; // host address
    private static int port; // port number
    private static PrintWriter out; // output stream
    private static BufferedReader in; // input stream

    /**
     * Main Method
     * @param args args[0] should specify host address, args[1] should specify host port
     * @throws SocketException
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws SocketException {
        // check arguments
        if (args.length < 4) {
            System.out.println("Wrong number of arguments");
            System.exit(1);
        }

        // set variables
        host = args[0];
        port = Integer.parseInt(args[1]);
        String oper = args[2];
        String dnsName = args[3];

        // open socket
        Socket socket = new Socket(host, port);

        // open streams
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // determine action
        if (oper.equals("register")) {
            if (args.length < 5) {
                System.out.println("Wrong number of arguments");
                System.exit(1);
            }
            String ipAddress = args[4];
            sendRegisterRequest(dnsName, ipAddress);
        } else if (oper.equals("lookup")) {
            sendLookupRequest(dnsName);
        } else {
            System.out.println("Invalid request");
            System.exit(2);
        }

        // receive and print response
        String response = receiveResponse();
        System.out.println(response);

        // close streams
        out.close();
        in.close();

        // close socket
        socket.close();
    }

    /**
     * Method that sends a register request
     * @param dnsName - DNS name to register
     * @param ipAddress - IP address to which the DNS name will be registered
     * @param socket - Communication socket
     * @throws UnknownHostException
     */
    private static void sendRegisterRequest(String dnsName, String ipAddress) {
        String request = "register " + dnsName + " " + ipAddress;
        try {
            out.println(request);
        } catch (IOException e) {
            System.out.println("Error sending register request");
        }
    }

    /**
     * Method that sends a lookup request
     * @param dnsName - DNS name to register
     * @param socket - Communication socket
     * @throws UnknownHostException
     */
    private static void sendLookupRequest(String dnsName) {
        String request = "lookup " + dnsName;
        try {
            out.println(request);
        } catch (IOException e) {
            System.out.println("Error sending lookup request");
        }
    }

    /**
     * Method that receives a response upon a request
     * @param socket - Communication socket
     * @return A string with the response
     * @throws UnknownHostException
     */
    private static String receiveResponse() {
        try {
            String response = in.readLine();
        } catch (IOException e) {
            System.out.println("Error receiving response");
        }

        return response;
    }
}
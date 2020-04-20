package client;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;

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
    public static void main(String[] args) throws UnknownHostException, IOException {
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
        String[] cypherSuites;

        if(oper.equals("lookup")){
            cypherSuites = Arrays.copyOfRange(args, 4, args.length);
        } else if (oper.equals("register")){
            cypherSuites = Arrays.copyOfRange(args, 5, args.length);
        } else {
            System.err.println("Invalid arguments passed");
            return;
        }


        // open socket
        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port);
        socket.setEnabledCipherSuites(cypherSuites);

        // open streams
        out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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
        socket.shutdownOutput();
        while(in.readLine() != null);
        out.close();
        in.close();

        // close socket
        socket.close();
    }

    /**
     * Method that sends a register request
     * @param dnsName - DNS name to register
     * @param ipAddress - IP address to which the DNS name will be registered
     * @throws UnknownHostException
     */
    private static void sendRegisterRequest(String dnsName, String ipAddress) {
        String request = "register " + dnsName + " " + ipAddress;
        out.println(request);
    }

    /**
     * Method that sends a lookup request
     * @param dnsName - DNS name to register
     * @throws UnknownHostException
     */
    private static void sendLookupRequest(String dnsName) {
        String request = "lookup " + dnsName;
        out.println(request);
    }

    /**
     * Method that receives a response upon a request
     * @return A string with the response
     * @throws UnknownHostException
     */
    private static String receiveResponse() {
        String response = null;
        try {
            response = in.readLine();
        } catch (IOException e) {
            System.out.println("Error receiving response");
        }

        return response;
    }
}
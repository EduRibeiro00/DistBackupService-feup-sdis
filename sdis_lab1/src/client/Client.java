package client;
import java.io.IOException;
import java.net.*;

/**
 * Client class
 */
public class Client {
    private static String host; // host address
    private static int port; // port number
    private static DatagramSocket socket; // socket that will be used for communication

    /**
     * Main Method
     * @param args args[1] should specify host address, args[2] should specify host port
     * @throws SocketException
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws SocketException, UnknownHostException {
        // check arguments
        if (args.length < 4) {
            System.out.println("Wrong number of arguments");
            System.exit(1);
        }

        // set variables
        host = args[1];
        port = Integer.parseInt(args[2]);
        String oper = args[3];
        String dnsName = args[4];

        // open socket
        socket = new DatagramSocket(port);

        // determine action
        if (oper == "register") {
            String ipAddress = args[5];
            sendRegisterRequest(dnsName, ipAddress, socket);
        } else if (oper == "lookup") {
            sendLookupRequest(dnsName, socket);
        }

        // receive and print response
        String response = receiveResponse(socket);
        System.out.println(response);
        socket.close();
    }

    /**
     * Method that sends a register request
     * @param dnsName - DNS name to register
     * @param ipAddress - IP address to which the DNS name will be registered
     * @param socket - Communication socket
     * @throws UnknownHostException
     */
    private static void sendRegisterRequest(String dnsName, String ipAddress, DatagramSocket socket) throws UnknownHostException {
        String request = "register " + dnsName + " " + ipAddress;
        byte[] buffer = request.getBytes();
        InetAddress address = InetAddress.getByName(host);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(packet);
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
    private static void sendLookupRequest(String dnsName, DatagramSocket socket) throws UnknownHostException {
        String request = "lookup " + dnsName;
        byte[] buffer = request.getBytes();
        InetAddress address = InetAddress.getByName(host);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(packet);
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
    private static String receiveResponse(DatagramSocket socket) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(host);
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            System.out.println("Error receiving response");
        }

        return new String(packet.getData());
    }
}

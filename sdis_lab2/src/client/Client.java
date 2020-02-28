package client;
import java.io.IOException;
import java.net.*;

/**
 * Client class
 */
public class Client {
    private static int TIMEOUT = 10000; // socket operation timeout (10 seconds)
    private static String mcast_addr; // multicast address
    private static int mcast_port; // multicast port number
    private static String lookup_addr; // lookup server's address
    private static int lookup_port; // lookup server's port number
    private static DatagramSocket socket; // socket that will be used for communication

    /**
     * Main Method
     * @param args args[0] should specify host address, args[1] should specify host port
     * @throws SocketException
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws IOException, UnknownHostException {
        // check arguments
        if (args.length < 4) {
            System.out.println("Wrong number of arguments");
            System.exit(1);
        }

        // set variables
        mcast_addr = args[0];
        mcast_port = Integer.parseInt(args[1]);
        String oper = args[2];
        String dnsName = args[3];

        // get lookup server's information
        DatagramPacket mcast_pckt = getLookupServer();
        lookup_addr = mcast_pckt.getAddress().getHostAddress();
        lookup_port = Integer.parseInt(new String(mcast_pckt.getData()).trim());

        String message = "multicast: " + mcast_addr + " " + Integer.toString(mcast_port) + ": " + lookup_addr + " " + Integer.toString(lookup_port);
        System.out.println(message);


        // open socket
        socket = new DatagramSocket();

        // determine action
        if (oper.equals("register")) {
            if (args.length < 5) {
                System.out.println("Wrong number of arguments");
                System.exit(1);
            }
            String ipAddress = args[4];
            sendRegisterRequest(dnsName, ipAddress, socket);
        } else if (oper.equals("lookup")) {
            sendLookupRequest(dnsName, socket);
        } else {
            System.out.println("Invalid request");
            System.exit(2);
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
        InetAddress address = InetAddress.getByName(lookup_addr);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, lookup_port);
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
        InetAddress address = InetAddress.getByName(lookup_addr);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, lookup_port);
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
        InetAddress address = InetAddress.getByName(lookup_addr);
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, lookup_port);
        try {
            socket.setSoTimeout(TIMEOUT);
            socket.receive(packet);
        } catch (IOException e) {
            System.out.println("Error receiving response from the lookup server");
        }

        return new String(packet.getData());
    }

    /**
     * Method that retreives the lookup server information in a datagram packet
     * @return A datagram packet containing the server's ip address and port
     */
    private static DatagramPacket getLookupServer() throws IOException, UnknownHostException {
        MulticastSocket mcast_socket = new MulticastSocket(mcast_port);
        InetAddress address = InetAddress.getByName(mcast_addr);
        mcast_socket.joinGroup(address);

        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            mcast_socket.setSoTimeout(TIMEOUT);
            mcast_socket.receive(packet);
        } catch (IOException e) {
            System.out.println("Error receiving response from the multicast server");
        }

        mcast_socket.leaveGroup(address);
        mcast_socket.close();

        return packet;
    }
}

package client;
import java.io.IOException;
import java.net.*;

public class Client {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        if (args.length < 4) {
            System.out.println("Wrong number of arguments");
            System.exit(1);
        }

        String host = args[1];
        int port = Integer.parseInt(args[2]);
        String oper = args[3];
        String dnsName = args[4];

        DatagramSocket socket = new DatagramSocket(port);

        if (oper == "register") {
            String ipAddress = args[5];
            sendRegisterRequest(host, port, dnsName, ipAddress, socket);
        } else if (oper == "lookup") {
            sendLookupRequest(host, port, dnsName, socket);
        }

        String response = receiveResponse(host, port, socket);
        System.out.println(response);
        socket.close();
    }

    private static void sendRegisterRequest(String host, int port, String dnsName, String ipAddress, DatagramSocket socket) throws UnknownHostException {
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

    private static void sendLookupRequest(String host, int port, String dnsName, DatagramSocket socket) throws UnknownHostException {
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

    private static String receiveResponse(String host, int port, DatagramSocket socket) throws UnknownHostException {
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

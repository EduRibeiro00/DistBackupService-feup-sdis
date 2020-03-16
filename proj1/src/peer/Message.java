package peer;

import java.util.Arrays;
import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;

public class Message {
    final String crlf = "\r\n";
    final String lastCRLF = "\r\n\r\n";
    Header header;
    String body;

    /**
     * Constructor for message receiving
     * @param data byte array with received data
     * @throws Exception
     */
    public Message(byte[] data) throws Exception {
        String message = Arrays.toString(data);
        String[] split = message.split(this.lastCRLF);

        if (split.length != 2){
            throw new Exception("Invalid message received");
        }

        this.header = new Header(Arrays.asList(split[0].split(" ")));
        this.body = split[1];
    }

    /**
     * Fills the Message class for message sending
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param repDeg the desired replication degree of the file's chunk (may be unused)
     */
    public Message(String version, MessageType msgType, String senderId, String fileId, int chunkNo, int repDeg, String body) throws NoSuchAlgorithmException {
        this.header = new Header(version, msgType, senderId, fileId, chunkNo, repDeg);
        this.body = body;
    }

    /**
     * Converts the full message to a byte array
     * @return byte array of the converted message
     * @throws NoSuchAlgorithmException
     */
    public byte[] convertToBytes() throws NoSuchAlgorithmException {
        String total = header.convertToString() + crlf + body;
        return total.getBytes();
    }

    /**
     * Sends a message in byte array format
     * @param mcast_socket the multicast socket used to send the message
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public void send(MulticastSocket mcast_socket) throws NoSuchAlgorithmException, IOException {
        byte[] content = this.convertToBytes();
        DatagramPacket mcast_packet = new DatagramPacket(content, content.length); //InetAddress address, int port ???
        mcast_socket.send(mcast_packet);
    }

    /**
     * @param mcast_socket multicast socket that will be used to
     */
    public void receive(MulticastSocket mcast_socket) throws Exception {
        byte[] buf = new byte[65000];
        DatagramPacket mcast_packet = new DatagramPacket(buf, 65000); //InetAddress address, int port ???
        mcast_socket.receive(mcast_packet);

        String message = Arrays.toString(mcast_packet.getData());
        String[] split = message.split(this.crlf);

        if (split.length != 2){
            throw new Exception("Invalid message received");
        }

        this.header = new Header(Arrays.asList(split[0].split(" ")));
        this.body = split[1];
    }
}

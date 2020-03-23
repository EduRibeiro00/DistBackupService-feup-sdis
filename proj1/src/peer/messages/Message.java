package peer.messages;

import java.util.Arrays;
import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;

public class Message {
    private final String crlf = "\r\n";
    private final String lastCRLF = "\r\n\r\n";
    private Header header;
    private String body;

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
    public Message(String version, MessageType msgType, int senderId, String fileId, int chunkNo, int repDeg, String body) throws NoSuchAlgorithmException {
        this.header = new Header(version, msgType, senderId, fileId, chunkNo, repDeg);
        this.body = body;
    }

    /**
     * Fills the Message class for message sending STORED, GETCHUNK and REMOVED messages
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     */
    public Message(String version, MessageType msgType, int senderId, String fileId, int chunkNo, String body) throws Exception {
        this.header = new Header(version, msgType, senderId, fileId, chunkNo);
        this.body = body;
    }

    /**
     * Fills the Message class for message sending STORED, GETCHUNK and REMOVED messages
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     */
    public Message(String version, MessageType msgType, int senderId, String fileId, int chunkNo) throws Exception {
        this.header = new Header(version, msgType, senderId, fileId, chunkNo);
        this.body = "";
    }

    /**
     * Fills the Message class for message sending DELETED messages
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     */
    public Message(String version, MessageType msgType, int senderId, String fileId) throws Exception {
        this.header = new Header(version, msgType, senderId, fileId);
        this.body = "";
    }

    /**
     * Converts the full message to a byte array
     * @return byte array of the converted message
     */
    private byte[] convertToBytes() {
        String total = header.toString() + crlf + body;
        return total.getBytes();
    }

    /**
     * Sends a message in byte array format
     * @param mcast_socket the multicast socket used to send the message
     * @throws IOException
     */
    public void send(MulticastSocket mcast_socket) throws IOException {
        byte[] content = this.convertToBytes();
        DatagramPacket mcast_packet = new DatagramPacket(content, content.length); //InetAddress address, int port ???
        mcast_socket.send(mcast_packet);

        System.out.println("Sending message: " + this.header);
    }

    public Header getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }
}

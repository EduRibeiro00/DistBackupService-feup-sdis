package peer.messages;

import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

/**
 * Class that represents a message that will be sent between peers to communicate
 */
public class Message implements Serializable {
    private final String crlf = "\r\n";           /** Carriage return and line feed, to  */
    private final String lastCRLF = "\r\n\r\n";   /** Double CRLF */
    private Header header;                        /** Header of the message */
    private byte[] body = new byte[0];            /** Body of the message */
    private InetAddress ipAddress;                /** IP address from where the message came */
    private int port;                             /** port number from where the message came */

    /**
     * Constructor for message receiving.
     * @param data byte array with received data
     */
    public Message(byte[] data) {
        String message = new String(data, StandardCharsets.ISO_8859_1);
        ArrayList<String> split = new ArrayList<>(Arrays.asList(message.split(this.lastCRLF, 2)));

        this.header = new Header(split.remove(0));

        if (split.size() != 0) {
            this.body = split.get(0).getBytes(StandardCharsets.ISO_8859_1);
        }
    }


    /**
     * Fills the Message class for sending PUTCHUNK.
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param repDeg the desired replication degree of the file's chunk (may be unused)
     */
    public Message(String version, MessageType msgType, int senderId, String fileId, int chunkNo, int repDeg, byte[] body) {
        this.header = new Header(version, msgType, senderId, fileId, chunkNo, repDeg);
        this.body = body;
    }


    /**
     * Fills the Message class for sending STORED, CHUNK messages.
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unused)
     */
    public Message(String version, MessageType msgType, int senderId, String fileId, int chunkNo, byte[] body) {
        this.header = new Header(version, msgType, senderId, fileId, chunkNo);
        this.body = body;
    }


    /**
     * Fills the Message class for sending CHUNK with enhancement.
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unused)
     * @param port port number of the TCP connection for sending the chunk
     */
    public Message(String version, MessageType msgType, int senderId, String fileId, int chunkNo, int port) {
        this.header = new Header(version, msgType, senderId, fileId, chunkNo, port);
    }


    /**
     * Fills the Message class for message sending REMOVED messages.
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     */
    public Message(String version, MessageType msgType, int senderId, String fileId, int chunkNo) {
        this.header = new Header(version, msgType, senderId, fileId, chunkNo);
    }


    /**
     * Fills the Message class for message sending DELETED messages.
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     */
    public Message(String version, MessageType msgType, int senderId, String fileId) {
        this.header = new Header(version, msgType, senderId, fileId);
    }


    /**
     * Fills the Message class for message sending DELETED messages.
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     */
    public Message(String version, MessageType msgType, int senderId) {
        this.header = new Header(version, msgType, senderId);
    }


    /**
     * Converts the full message to a byte array.
     * @return byte array of the converted message
     */
    public byte[] convertToBytes() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(header.toString().getBytes(StandardCharsets.ISO_8859_1));
        stream.write(crlf.getBytes(StandardCharsets.ISO_8859_1));
        stream.write(body);
        return stream.toByteArray();
    }


    /**
     * Method that sends the message through a UDP multicast channel, described by an IP address and a port.
     * @param ipAddress IP address of the multicast channel
     * @param port port number of the multicast channel
     * @throws IOException
     */
    public void send(String ipAddress, int port) throws IOException {

        // because sockets should not be shared between threads, each time a message is sent, a new socket object
        // is created
        MulticastSocket mCastSkt = new MulticastSocket(port);
        mCastSkt.setTimeToLive(1);

        byte[] content = this.convertToBytes();
        DatagramPacket mcast_packet = new DatagramPacket(content, content.length, InetAddress.getByName(ipAddress), port);
        mCastSkt.send(mcast_packet);

        System.out.println("Sending message: " + this.header);
    }


    /**
     * Method that sends the message through a TCP connection, using a buffered output stream
     * @param out stream to send the message
     */
    public void send(BufferedOutputStream out) throws IOException {
        byte[] messageBytes = this.convertToBytes();

        // write
        out.write(messageBytes, 0, messageBytes.length);
        out.flush();

        System.out.println("Sending message (TCP): " + this.header);
    }


    /**
     * Retrieves the header of the message.
     * @return header of the message
     */
    public Header getHeader() {
        return header;
    }


    /**
     * Retrieves the body of the message (if any).
     * @return body of the message
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Retrieves the IP address from where the message came from.
     * @return IP address
     */
    public InetAddress getIpAddress() {
        return ipAddress;
    }


    /**
     * Sets the IP address from where the message came from.
     * @param ipAddress IP address
     */
    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the port number from where the message came from.
     * @return port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number from where the message came from.
     * @param port port number
     */
    public void setPort(int port) {
        this.port = port;
    }
}

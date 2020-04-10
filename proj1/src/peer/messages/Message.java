package peer.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;
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
    private InetAddress ipAddress;
    private int port;

    /**
     * Constructor for message receiving
     * @param data byte array with received data
     */
    public Message(byte[] data) {
        String message = new String(data, StandardCharsets.ISO_8859_1);
        ArrayList<String> split = new ArrayList<>(Arrays.asList(message.split(this.lastCRLF, 2)));

        this.header = new Header(new ArrayList<>(Arrays.asList(split.remove(0).split(" "))));

        if (split.size() != 0) {
            this.body = split.get(0).getBytes(StandardCharsets.ISO_8859_1);
        }
    }


    /**
     * Fills the Message class for sending PUTCHUNK
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
     * Fills the Message class for message sending STORED, GETCHUNK messages
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
     * Fills the Message class for message sending REMOVED messages
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
     * Fills the Message class for message sending DELETED messages
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     */
    public Message(String version, MessageType msgType, int senderId, String fileId) {
        this.header = new Header(version, msgType, senderId, fileId);
    }


    /**
     * Fills the Message class for message sending DELETED messages
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     */
    public Message(String version, MessageType msgType, int senderId) {
        this.header = new Header(version, msgType, senderId);
    }



    /**
     * Converts the full message to a byte array
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
     * send
     * @param ipAddress
     * @param port
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
     * Retrieves the header of the message.
     */
    public Header getHeader() {
        return header;
    }


    /**
     * Retrieves the body of the message (if any).
     * @return
     */
    public byte[] getBody() {
        return body;
    }

    /**
     *
     * @return
     */
    public InetAddress getIpAddress() {
        return ipAddress;
    }

    /**
     *
     * @param ipAddress
     */
    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }
}

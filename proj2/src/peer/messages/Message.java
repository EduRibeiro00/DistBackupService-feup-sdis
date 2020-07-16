package peer.messages;

import peer.chord.ChordNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class that represents a message that will be sent between peers to communicate
 */
public class Message implements Serializable {
    /**
     * Carriage return and line feed
     */
    private final String crlf = "\r\n";

    /**
     * Double CRLF
     */
    private final String lastCRLF = "\r\n\r\n";

    /**
     * Header of the message
     */
    private final Header header;

    /**
     * Body of the message
     */
    private byte[] body = new byte[0];

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
     * Fills the Message class for sending PUTCHUNK messages.
     *
     * @param msgType  the type of message to be sent
     * @param fileId   the file identifier in the backup service, as the result of SHA256
     * @param chunkNo  the chunk number of the specified file
     */
    public Message(MessageType msgType, String fileId, int chunkNo, int replication, String ipAddress, int port, byte[] body) {
        this.header = new Header(msgType, fileId, chunkNo, replication, ipAddress, port);
        this.body = body;
    }

    /**
     * Fills the Message class for sending GIVECHUNK messages.
     *
     * @param msgType  the type of message to be sent
     * @param fileId   the file identifier in the backup service, as the result of SHA256
     * @param chunkNo  the chunk number of the specified file
     */
    public Message(MessageType msgType, String fileId, int chunkNo, String ipAddress, int port, int barrierId, byte[] body) {
        this.header = new Header(msgType, fileId, chunkNo, ipAddress, port, barrierId);
        this.body = body;
    }

    /**
     * Fills the Message class for message sending STORED and REMOVED messages.
     *
     * @param msgType  the type of message to be sent
     * @param fileId   the file identifier in the backup service, as the result of SHA256
     * @param chunkNo  the chunk number of the specified file
     */
    public Message(MessageType msgType, String fileId, int chunkNo) {
        this.header = new Header(msgType, fileId, chunkNo);
    }

    /**
     * Fills the Message class for sending CHUNK messages.
     *
     * @param msgType  the type of message to be sent
     * @param fileId   the file identifier in the backup service, as the result of SHA256
     * @param chunkNo  the chunk number of the specified file
     */
    public Message(MessageType msgType, String fileId, int chunkNo, byte[] body) {
        this.header = new Header(msgType, fileId, chunkNo);
        this.body = body;
    }

    /**
     * Fills the Message class for sending GETCHUNK messages.
     *
     * @param msgType  the type of message to be sent
     * @param fileId   the file identifier in the backup service, as the result of SHA256
     * @param chunkNo  the chunk number of the specified file
     */
    public Message(MessageType msgType, String fileId, int chunkNo, String ipAddress, int port) {
        this.header = new Header(msgType, fileId, chunkNo, ipAddress, port);
    }

    /**
     * Fills the Message class for message sending DELETE messages.
     *
     * @param msgType  the type of message to be sent
     * @param fileId   the file identifier in the backup service, as the result of SHA256
     */
    public Message(MessageType msgType, String fileId, String ipAddress, int port) {
        this.header = new Header(msgType, fileId, ipAddress, port);
    }

    /**
     * Fills the Message class for message sending DELETED messages.
     *
     * @param msgType  the type of message to be sent
     * @param fileId   the file identifier in the backup service, as the result of SHA256
     */
    public Message(MessageType msgType, String fileId) {
        this.header = new Header(msgType, fileId);
    }

    /**
     * Fills the Message class for message sending CHECK_ACTIVE messages.
     *
     * @param msgType  the type of message to be sent
     */
    public Message(MessageType msgType) {
        this.header = new Header(msgType);
    }

    /**
     * Fills the Message class for message sending FIND_SUCC messages.
     *
     * @param msgType  the type of message to be sent
     * @param key Chord key
     * @param ipAddress IP address
     * @param port Port number
     */
    public Message(MessageType msgType, int key, String ipAddress, int port) {
        this.header = new Header(msgType, key, ipAddress, port);
    }

    /**
     *  Fills the Message class for message sending RTRN_PRED, GET_PRED, SET_SUCC, SET_PRED and NOTIFY messages.
     *
     * @param msgType  the type of message to be sent
     * @param node Chord node to send
     */
    public Message(MessageType msgType, ChordNode node) {
        this.header = new Header(msgType, node);
    }

    /**
     *  Fills the Message class for message sending RTRN_SUCC messages.
     *
     * @param msgType  the type of message to be sent
     * @param node Chord node to send
     * @param keyHash key that we wanted
     */
    public Message(MessageType msgType, ChordNode node, int keyHash) {
        this.header = new Header(msgType, node, keyHash);
    }

    /**
     * Converts the full message to a byte array.
     *
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
     * Retrieves the header of the message.
     *
     * @return header of the message
     */
    public Header getHeader() {
        return header;
    }


    /**
     * Retrieves the body of the message (if any).
     *
     * @return body of the message
     */
    public byte[] getBody() {
        return body;
    }


    public void setSenderId(int senderId) {
        this.header.setSenderId(senderId);
    }
}

package peer.messages;

import peer.chord.ChordNode;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Class responsible for handling the header of a message either when receiving or when sending
 */
public class Header implements Serializable {
    private int replication;                    // Replication of the message
    private final MessageType messageType;      // Type of the message
    private int senderId;                       // ID of the sender peer
    private String fileId;                      // ID of the file
    private int chunkNo;                        // Number of the chunk
    private int key;                            // Chord key sent/received
    private ChordNode node;                     // Node to be sent
    private String ipAddress;                   // IP address of the node to contact
    private int port;                           // port number of the node to contact
    private int barrierId;                      // node ID after which the message should not be propagated

    /**
     * Fills the Header class based on the elements of the header list, for message receiving
     * @param fullHeader the header received in a message
     */
    public Header(String fullHeader) throws IllegalArgumentException {
        ArrayList<String> headerLines = new ArrayList<>(Arrays.asList(fullHeader.split("\r\n")));

        if(headerLines.size() < 1) {
            throw new IllegalArgumentException("Invalid message header received");
        }

        ArrayList<String> headerMain = new ArrayList<>(Arrays.asList(headerLines.remove(0).split("\\s+")));

        // No point in processing the rest if we don't know any message with header size < 2
        if(headerMain.size() < 2) {
            throw new IllegalArgumentException("Invalid message header received");
        }

        this.messageType = MessageType.valueOf(headerMain.remove(0).trim());
        this.senderId = Integer.parseInt(headerMain.remove(0).trim());

        switch (this.messageType) {
            case PUTCHUNK:
                this.fileId = headerMain.remove(0).trim();
                this.chunkNo = Integer.parseInt(headerMain.remove(0).trim());
                this.replication = Integer.parseInt(headerMain.remove(0).trim());
                this.ipAddress = headerMain.remove(0).trim();
                this.port = Integer.parseInt(headerMain.remove(0).trim());
                break;
            case GIVECHUNK:
                this.fileId = headerMain.remove(0).trim();
                this.chunkNo = Integer.parseInt(headerMain.remove(0).trim());
                this.ipAddress = headerMain.remove(0).trim();
                this.port = Integer.parseInt(headerMain.remove(0).trim());
                this.barrierId = Integer.parseInt(headerMain.remove(0).trim());
                break;
            case GETCHUNK:
                this.fileId = headerMain.remove(0).trim();
                this.chunkNo = Integer.parseInt(headerMain.remove(0).trim());
                this.ipAddress = headerMain.remove(0).trim();
                this.port = Integer.parseInt(headerMain.remove(0).trim());
                break;
            case CHUNK:
            case STORED:
            case REMOVED:
                this.fileId = headerMain.remove(0).trim();
                this.chunkNo =  Integer.parseInt(headerMain.remove(0).trim());
                break;
            case DELETE:
                this.fileId = headerMain.remove(0).trim();
                this.ipAddress = headerMain.remove(0).trim();
                this.port = Integer.parseInt(headerMain.remove(0).trim());
                break;
            case DELETED:
                this.fileId = headerMain.remove(0).trim();
                break;
            case FIND_SUCC:
                this.key = Integer.parseInt(headerMain.remove(0).trim());
                this.ipAddress = headerMain.remove(0).trim();
                this.port = Integer.parseInt(headerMain.remove(0).trim());
                break;
            case RTRN_SUCC:
                this.key = Integer.parseInt(headerMain.remove(0).trim());
            case SET_SUCC:
            case SET_PRED:
            case GET_PRED:
            case RTRN_PRED:
            case NOTIFY:
                int id = Integer.parseInt(headerMain.remove(0).trim());
                String ipAddress = headerMain.remove(0).trim();
                int portMC = Integer.parseInt(headerMain.remove(0).trim());
                int portMDB = Integer.parseInt(headerMain.remove(0).trim());
                int portMDR = Integer.parseInt(headerMain.remove(0).trim());
                int portChord = Integer.parseInt(headerMain.remove(0).trim());
                this.node = new ChordNode(ipAddress, portMC, portMDB, portMDR, portChord, id);
                break;
            default:
                break;
        }
    }

    /**
     * Fills the Header class for message sending (PUTCHUNK)
     * @param msgType the type of message to be sent
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file
     * @param ipAddress IP address of the sending peer
     * @param port Port number of the sending peer
     */
    public Header(MessageType msgType, String fileId, int chunkNo, int replication, String ipAddress, int port) throws IllegalArgumentException {
        if(msgType != MessageType.PUTCHUNK) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.replication = replication;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Fills the Header class for message sending (GIVECHUNK)
     * @param msgType the type of message to be sent
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file
     * @param ipAddress IP address of the sending peer
     * @param port Port number of the sending peer
     * @param barrierId Barrier ID
     */
    public Header(MessageType msgType, String fileId, int chunkNo, String ipAddress, int port, int barrierId) throws IllegalArgumentException {
        if(msgType != MessageType.GIVECHUNK) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.ipAddress = ipAddress;
        this.port = port;
        this.barrierId = barrierId;
    }


    /**
     * Fills the Header class for message sending (GETCHUNK)
     * @param msgType the type of message to be sent
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file
     * @param ipAddress IP address of the sending peer
     * @param port Port number of the sending peer
     */
    public Header(MessageType msgType, String fileId, int chunkNo, String ipAddress, int port) throws IllegalArgumentException {
        if(msgType != MessageType.GETCHUNK) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Fills the Header class for message sending (STORED, REMOVED, CHUNK)
     * @param msgType the type of message to be sent
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     */
    public Header(MessageType msgType, String fileId, int chunkNo) throws IllegalArgumentException {
        if(msgType != MessageType.CHUNK && msgType != MessageType.STORED && msgType != MessageType.REMOVED) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
    }

    /**
     * Fills the Header class for Chord message sending (DELETE)
     */
    public Header(MessageType msgType, String fileId, String ipAddress, int port) throws IllegalArgumentException {
        if(msgType != MessageType.DELETE) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
        this.fileId = fileId;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Fills the Header class for message sending (DELETED)
     * @param msgType the type of message to be sent
     * @param fileId the file identifier in the backup service, as the result of SHA256
     */
    public Header(MessageType msgType, String fileId) throws IllegalArgumentException {
        if(msgType != MessageType.DELETED) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
        this.fileId = fileId;
    }

    /**
     * Fills the Header class for Chord message sending
     * @param msgType the type of message to be sent
     * @param key Chord key
     * @param ipAddress IP address of the node to be contacted after its successor is found
     * @param port Port number of the node to be contacted after its successor is found
     */
    public Header(MessageType msgType, int key, String ipAddress, int port) throws IllegalArgumentException {
        if(msgType != MessageType.FIND_SUCC) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
        this.key = key;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Fills the Header class for message sending (CHECK_ACTIVE)
     * @param msgType the type of message to be sent
     */
    public Header(MessageType msgType) throws IllegalArgumentException {
        if(msgType != MessageType.CHECK_ACTIVE) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
    }

    /**
     * Fills the Header class for message sending with a Chord node
     * @param msgType the type of message to be sent
     * @param node Chord node to send
     */
    public Header(MessageType msgType, ChordNode node) {
        if(msgType != MessageType.RTRN_PRED
                && msgType != MessageType.NOTIFY
                && msgType != MessageType.GET_PRED
                && msgType != MessageType.SET_PRED
                && msgType != MessageType.SET_SUCC) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
        this.node = node;
    }

    /**
     * Fills the Header class for message sending with a Chord node
     * @param msgType the type of message to be sent
     * @param node Chord node to send
     * @param keyHash key that we wanted
     */
    public Header(MessageType msgType, ChordNode node, int keyHash) {
        if(msgType != MessageType.RTRN_SUCC) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.messageType = msgType;
        this.key = keyHash;
        this.node = node;
    }

    /**
     * Retrieves the message type.
     */
    public MessageType getMessageType() {
        return messageType;
    }


    /**
     * Retrieves the ID of the sender peer.
     */
    public int getSenderId() {
        return senderId;
    }


    /**
     * Retrieves the ID of the file.
     */
    public String getFileId() {
        return fileId;
    }


    /**
     * Retrieves the chunk number.
     */
    public int getChunkNo() {
        return chunkNo;
    }


    /**
     * Retrieves the Chord key received
     */
    public int getKey() {
        return key;
    }

    /**
     * Retrieves the Chord node
     */
    public ChordNode getNode() {
        return node;
    }

    /**
     * Retrieves the IP address of the node that should be contacted
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Retrieves the port number of the node that should be contacted
     */
    public int getPort() {
        return port;
    }

    /**
     * Retrieves the replication number desired for the chunk
     */
    public int getReplication() {
        return replication;
    }

    /**
     * Sets the
     * @param replication the new replication
     */
    public void setReplication(int replication) {
        this.replication = replication;
    }


    public int getBarrierId() {
        return barrierId;
    }

    /**
     * Creates a message header in string format for peer-peer communication
     * @return the message header in a string
     */
    @Override
    public String toString() {
        String msgTypeStr = messageType.name();
        String header = msgTypeStr + " " + senderId;

        switch(messageType) {
            case PUTCHUNK:
                header +=  " " + fileId + " " + chunkNo + " " + replication + " " + ipAddress + " " + port;
                break;
            case GIVECHUNK:
                header +=  " " + fileId + " " + chunkNo + " " + ipAddress + " " + port + " " + barrierId;
                break;
            case GETCHUNK:
                header +=  " " + fileId + " " + chunkNo + " " + ipAddress + " " + port;
                break;
            case CHUNK:
            case STORED:
            case REMOVED:
                header +=  " " + fileId + " " + chunkNo;
                break;
            case DELETE:
                header +=  " " + fileId + " " + ipAddress + " " + port;
                break;
            case DELETED:
                header +=  " " + fileId;
                break;
            case FIND_SUCC:
                header += " " + key + " " + ipAddress + " " + port;
                break;
            case RTRN_SUCC:
                header += " " + key;
            case SET_SUCC:
            case SET_PRED:
            case GET_PRED:
            case RTRN_PRED:
            case NOTIFY:
                header += " " + node.getId() + " " + node.getIpAddress() + " " + node.getPortMC() + " " + node.getPortMDB() + " " + node.getPortMDR() + " " + node.getPortChord();
                break;
        }

        header += " \r\n";

        return header;
    }


    /**
     * Hashes a string using the SHA-256 cryptographic function
     * @param str the string to be hashed
     * @return an array of bytes resultant of the hash
     * @throws NoSuchAlgorithmException when this peer doesn't have the algorithm
     */
    public static String encodeFileId(String str) throws NoSuchAlgorithmException {
        byte[] fileId = getSHA256(str);
        StringBuilder result = new StringBuilder();
        for(byte bt : fileId) {
            int decimal = (int) bt & 0xff;  // bytes widen to int, need mask as to prevent sign extension
            String hex = Integer.toHexString(decimal);
            result.append(hex);
        }
        return result.toString();
    }


    /**
     * Hashes a string using the SHA-256 cryptographic function
     * @param str the string to be hashed
     * @return an array of bytes resultant of the hash
     * @throws NoSuchAlgorithmException when this peer doesn't have the algorithm
     */
    private static byte[] getSHA256(String str) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(str.getBytes(StandardCharsets.UTF_8));
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }
}

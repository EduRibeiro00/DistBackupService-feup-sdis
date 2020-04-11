package peer.messages;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Class responsible for handling the header of a message either when receiving or when sending
 */
public class Header implements Serializable {
    private String version;             /** Version of the protocol */
    private MessageType messageType;    /** Type of the message */
    private int senderId;               /** ID of the sender peer */
    private String fileId;              /** ID of the file */
    private int chunkNo;                /** Number of the chunk */
    private int replicationDeg;         /** Replication degree */
    private List<String> other;         /** Other fields of the header */
    private int portNumber;             /** Port number for TCP connection */


    /**
     * Fills the Header class based on the elements of the header list, for message receiving
     * @param fullHeader the header received in a message
     */
    public Header(String fullHeader) throws IllegalArgumentException {
        ArrayList<String> headerLines = new ArrayList<>(Arrays.asList(fullHeader.split("\r\n")));

        if(headerLines.size() < 1) {
            throw new IllegalArgumentException("Invalid message header received");
        }

        ArrayList<String> headerMain = new ArrayList<>(Arrays.asList(headerLines.remove(0).split(" ")));

        // No point in processing the rest if we don't know any message with header size < 3
        if(headerMain.size() < 3) {
            throw new IllegalArgumentException("Invalid message header received");
        }

        this.version = headerMain.remove(0);
        this.messageType = MessageType.valueOf(headerMain.remove(0));
        this.senderId = Integer.parseInt(headerMain.remove(0));

        switch (this.messageType) {
            case GREETINGS:
                this.fileId = "";
                break;
            case CHUNK:
                if(this.version.equals("1.1")) {
                    if(headerLines.size() < 1) {
                        throw new IllegalArgumentException("Invalid message header received");
                    }
                    this.portNumber = Integer.parseInt(headerLines.get(0).trim());
                }
            case STORED: case GETCHUNK: case REMOVED:
                this.fileId = headerMain.remove(0);
                if(headerMain.size() != 1) {
                    throw new IllegalArgumentException("Invalid message header received");
                }
                this.chunkNo = Integer.parseInt(headerMain.remove(0));
                break;
            case PUTCHUNK:
                this.fileId = headerMain.remove(0);
                if(headerMain.size() != 2) {
                    throw new IllegalArgumentException("Invalid message header received");
                }
                this.chunkNo = Integer.parseInt(headerMain.remove(0));
                this.replicationDeg = Integer.parseInt(headerMain.remove(0));
                break;
            default:
                this.fileId = headerMain.remove(0);
                break;
        }

        this.other = headerMain;
    }

    /**
     * Fills the Header class for message sending
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param repDeg_portNumber the desired replication degree of the file's chunk (may be unused) or the portNumber (only used in enhancements)
     */
    public Header(String version, MessageType msgType, int senderId, String fileId, int chunkNo, int repDeg_portNumber) throws IllegalArgumentException {
        if(msgType != MessageType.PUTCHUNK && msgType != MessageType.CHUNK) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.version = version;
        this.messageType = msgType;
        this.senderId = senderId;
        this.chunkNo = chunkNo;

        if(msgType == MessageType.PUTCHUNK) {
            this.replicationDeg = repDeg_portNumber;
        } else {
            this.portNumber = repDeg_portNumber;
        }

        this.fileId = fileId;
    }

    /**
     * Fills the Header class for message sending without repDeg
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     */
    public Header(String version, MessageType msgType, int senderId, String fileId, int chunkNo) throws IllegalArgumentException {
        if(msgType != MessageType.STORED && msgType != MessageType.GETCHUNK && msgType != MessageType.REMOVED && msgType != MessageType.CHUNK) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.version = version;
        this.messageType = msgType;
        this.senderId = senderId;
        this.chunkNo = chunkNo;
        this.replicationDeg = -1;

        this.fileId = fileId;
    }

    /**
     * Fills the Header class for message sending without chunkNo and RepDeg
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     */
    public Header(String version, MessageType msgType, int senderId, String fileId) throws IllegalArgumentException {
        if(msgType != MessageType.DELETE && msgType != MessageType.DELETED) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.version = version;
        this.messageType = msgType;
        this.senderId = senderId;
        this.chunkNo = -1;
        this.replicationDeg = -1;

        this.fileId = fileId;
    }


    /**
     * Fills the Header class for message sending without chunkNo and RepDeg
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     */
    public Header(String version, MessageType msgType, int senderId) throws IllegalArgumentException {
        if(msgType != MessageType.GREETINGS) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.version = version;
        this.messageType = msgType;
        this.senderId = senderId;
        this.chunkNo = -1;
        this.replicationDeg = -1;
        this.fileId = "";
    }

    /**
     * Retrieves the version of the message.
     */
    public String getVersion() {
        return version;
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
     * Retrieves the replication degree.
     */
    public int getReplicationDeg() {
        return replicationDeg;
    }

    /**
     * Retrieves the port Number
     */
    public int getPortNumber() {
        return portNumber;
    }

    /**
     * Retrieves the other fields of the header (if any).
     * @return
     */
    public List<String> getOther() {
        return other;
    }


    /**
     * Creates a message header in string format for peer-peer communication
     * @return the message header in a string
     */
    @Override
    public String toString() {
        String msgTypeStr = messageType.name();

        String header = version + " " + msgTypeStr + " " + senderId + " " + fileId;
        switch(messageType) {
            case PUTCHUNK:
                header += " " + chunkNo + " " + replicationDeg;
                break;
            case CHUNK:
            case STORED:
            case GETCHUNK:
            case REMOVED:
                header += " " + chunkNo;
                break;
            case DELETE:
            case DELETED:
            case GREETINGS:
                break;
        }

        header += " \r\n"; // Warning - In case we decided to use "multiple headers inside of the same header" the last <CRLF> needs to be removed

        if(messageType == MessageType.CHUNK && version.equals("1.1")){
            header += this.portNumber + " \r\n";
        }
        return header;
    }


    /**
     * Hashes a string using the SHA-256 cryptographic function
     * @param str the string to be hashed
     * @return an array of bytes resultant of the hash
     * @throws NoSuchAlgorithmException
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
     * @throws NoSuchAlgorithmException
     */
    private static byte[] getSHA256(String str) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
        return encodedhash;
    }
}

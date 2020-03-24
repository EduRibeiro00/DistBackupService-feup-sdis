package peer.messages;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Class responsible for handling the header of a message either when receiving or when sending
 */
public class Header {
    private String version;             /** Version of the protocol */
    private MessageType messageType;    /** Type of the message */
    private int senderId;               /** ID of the sender peer */
    private String fileId;              /** ID of the file */
    private int chunkNo;                /** Number of the chunk */
    private int replicationDeg;         /** Replication degree */
    private List<String> other;         /** Other fields of the header */

    private static List<String> messageTypeList = Arrays.asList(
        "PUTCHUNK",
        "STORED",
        "GETCHUNK",
        "CHUNK",
        "DELETE",
        "REMOVED"
    ); /** Different types of messages */


    /**
     * Fills the Header class based on the elements of the header list, for message receiving
     * @param header a list of elements received from a peer
     */
    public Header(ArrayList<String> header) throws IllegalArgumentException {
        // No point in processing the rest if we don't know any message with header size < 4
        if(header.size() < 4 || header.size() > 6) {
            throw new IllegalArgumentException("Invalid message header received");
        }

        this.version = header.remove(0);
        this.messageType = MessageType.valueOf(header.remove(0));
        this.senderId = Integer.parseInt(header.remove(0));
        this.fileId = header.remove(0);

        switch (this.messageType) {
            case STORED: case GETCHUNK: case CHUNK: case REMOVED:
                if(header.size() != 1) {
                    throw new IllegalArgumentException("Invalid message header received");
                }
                this.chunkNo = Integer.parseInt(header.remove(0));
                break;
            case PUTCHUNK:
                if(header.size() != 2) {
                    throw new IllegalArgumentException("Invalid message header received");
                }
                this.chunkNo = Integer.parseInt(header.remove(0));
                this.replicationDeg = Integer.parseInt(header.remove(0));
                break;
            default:
                break;
        }

        this.other = header;
    }

    /**
     * Fills the Header class for message sending
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param repDeg the desired replication degree of the file's chunk (may be unused)
     */
    public Header(String version, MessageType msgType, int senderId, String fileId, int chunkNo, int repDeg) throws NoSuchAlgorithmException {
        if(msgType != MessageType.PUTCHUNK) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.version = version;
        this.messageType = msgType;
        this.senderId = senderId;
        this.chunkNo = chunkNo;
        this.replicationDeg = repDeg;

        byte[] fileIdByte = getSHA256(fileId);
        this.fileId = encodeFileId(fileIdByte);
    }

    /**
     * Fills the Header class for message sending without repDeg
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     */
    public Header(String version, MessageType msgType, int senderId, String fileId, int chunkNo) throws NoSuchAlgorithmException {
        if(msgType == MessageType.DELETE || msgType == MessageType.PUTCHUNK) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.version = version;
        this.messageType = msgType;
        this.senderId = senderId;
        this.chunkNo = chunkNo;
        this.replicationDeg = -1;

        byte[] fileIdByte = getSHA256(fileId);
        this.fileId = encodeFileId(fileIdByte);
    }

    /**
     * Fills the Header class for message sending without chunkNo and RepDeg
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     */
    public Header(String version, MessageType msgType, int senderId, String fileId) throws Exception {
        if(msgType != MessageType.DELETE) {
            throw new IllegalArgumentException("Invalid message header");
        }
        this.version = version;
        this.messageType = msgType;
        this.senderId = senderId;
        this.chunkNo = -1;
        this.replicationDeg = -1;

        byte[] fileIdByte = getSHA256(fileId);
        this.fileId = encodeFileId(fileIdByte);
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
            case STORED:
            case GETCHUNK:
            case CHUNK:
            case REMOVED:
                header += " " + chunkNo;
                break;
            case DELETE:
                break;
        }
        header += " \r\n"; // Warning - In case we decided to use "multiple headers inside of the same header" the last <CRLF> needs to be removed
        return header;
    }


    /**
     * Encodes the file ID so that it can be sent in a message header
     * @param fileId the file identifier in the backup service
     * @return the encoded file ID in a string
     */
    private String encodeFileId(byte[] fileId) {
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
    private byte[] getSHA256(String str) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
        return encodedhash;
    }
}

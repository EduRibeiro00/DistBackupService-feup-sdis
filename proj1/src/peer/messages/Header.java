package peer.messages;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Class responsible for handling the header of a message
 * either when receiving or when sending
 */
public class Header {
    private String version;
    private MessageType messageType;
    private String senderId;
    private String fileId;
    private int chunkNo;
    private int replicationDeg;
    private List<String> other;

    private static List<String> messageTypeList = Arrays.asList("PUTCHUNK", "STORED", "GETCHUNK", "CHUNK", "DELETE", "REMOVED");

    /**
     * Fills the Header class based on the elements of the header list, for message receiving
     * @param header a list of elemtns received from a peer
     */
    public Header(List<String> header) throws Exception {
        // No point in processing the rest if we don't know any message with header size < 4
        if(header.size() < 4 || header.size() > 6) {
            throw new Exception("Invalid message header received");
        }

        this.version = header.remove(0);
        this.messageType = MessageType.valueOf(header.remove(0));
        this.senderId = header.remove(0);
        this.fileId = header.remove(0);

        this.chunkNo = Integer.parseInt(header.remove(0));
        this.replicationDeg = Integer.parseInt(header.remove(0));
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
    public Header(String version, MessageType msgType, String senderId, String fileId, int chunkNo, int repDeg) throws NoSuchAlgorithmException {
        this.version = version;
        this.messageType = msgType;
        this.senderId = senderId;
        this.chunkNo = chunkNo;
        this.replicationDeg = repDeg;

        byte[] fileIdByte = getSHA256(fileId);
        this.fileId = encodeFileId(fileIdByte);
    }

    public String getVersion() {
        return version;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getFileId() {
        return fileId;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public int getReplicationDeg() {
        return replicationDeg;
    }

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
     * @return the enconded file ID in a string
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

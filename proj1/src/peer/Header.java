package peer;

import java.util.List;

/**
 * Class responsible for handling the header of a message
 * either when receiving or when sending
 */
public class Header {
    int size;
    String version;
    MessageType messageType;
    String senderId;
    String fileId;
    int chunkNo;
    int replicationDeg;
    List<String> other;

    /**
     * Fills the Header class based on the elements of the header list
     * @param header
     */
    public Header(List<String> header) throws Exception {
        this.size = header.size();

        // No point in processing the rest if we don't know any message with header size < 4
        if(this.size < 4 || this.size > 6) {
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
}

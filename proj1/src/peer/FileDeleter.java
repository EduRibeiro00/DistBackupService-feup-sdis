package peer;

import peer.messages.Message;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that encompasses all structures and information needed to warn a peer to delete chunks of a certain file.
 */
public class FileDeleter implements Serializable {
    private String ipAddress;  /** IP address to send the messages to */
    private int port;          /** port to send the messages to */

    /**
     * Stores the messages that are supposed to be sent to the peer, organized by file identifier.
     * key = fileId
     * value = delete message for that file
     */
    private ConcurrentHashMap<String, Message> fileToDeletes;

    /**
     * Empty constructor of the file deleter.
     */
    public FileDeleter() {
        this.ipAddress = "";
        this.port = -1;
        this.fileToDeletes = new ConcurrentHashMap<>();
    }

    /**
     * Regular constructor of the file deleter.
     * @param ipAddress IP address to send the messages to
     * @param port port to send the messages to
     */
    public FileDeleter(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.fileToDeletes = new ConcurrentHashMap<>();
    }

    /**
     * Adds a message to the table of delete messages that need to be sent to that peer.
     * @param msg new delete message to be sent
     */
    public void addMessage(Message msg) {
        this.fileToDeletes.put(msg.getHeader().getFileId(), msg);
    }

    /**
     * Remove the delete message associated with a specific fileId.
     * @param fileId file identifier
     * @return true if the peer does not have any delete messages to receive; false otherwise
     */
    public boolean removeMessages(String fileId) {
        this.fileToDeletes.remove(fileId);
        return this.fileToDeletes.isEmpty();
    }

    /**
     * Sends all kept delete messages to the peer.
     */
    public void sendMessages() {
        for(Map.Entry<String, Message> entry : fileToDeletes.entrySet()) {
            try {
                entry.getValue().send(this.ipAddress, this.port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fileToDeletes = new ConcurrentHashMap<>();
    }

}

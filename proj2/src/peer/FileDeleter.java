package peer;

import peer.jsse.SenderThread;
import peer.messages.Message;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that encompasses all structures and information needed to warn a peer to delete chunks of a certain file.
 */
public class FileDeleter implements Serializable {
    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * id of the peer we want to send messages to
     */
    private final int peerId;

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
        this.peerId = -1;
        this.fileToDeletes = new ConcurrentHashMap<>();
    }

    /**
     * Regular constructor of the file deleter.
     * @param peerId id of the peer we want to send the delete
     */
    public FileDeleter(int peerId) {
        this.peerId = peerId;
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
     * @return a set of the files the peer with peerid needs to delete
     */
    public Set<Map.Entry<String, Message>> getFileToDeletes() {
        return fileToDeletes.entrySet();
    }

    /**
     * @return the id of the peer who still has files to delete
     */
    public int getPeerId() {
        return peerId;
    }
}

package peer.protocols;

import peer.ChunkManager;
import peer.FileManager;
import peer.messages.Header;
import peer.messages.Message;

/**
 * Class that represents the protocol that is being used by the peer
 */
public abstract class Protocol {
    protected final int TIMEOUT = 1000;        /** timeout constant */
    protected final int CHUNK_SIZE = 64000;    /** chunk size constant */
    protected int peerID;                      /** peer identifier */
    protected ChunkManager chunkManager;       /** chunk manager instance */
    protected FileManager fileManager;         /** file manager instance */
    protected String protocolVersion;          /** protocol version */

    protected String ipAddressMC;              /** IP address of the control channel */
    protected int portMC;                      /** port of the control channel */
    protected String ipAddressMDB;             /** IP address of the data backup channel */
    protected int portMDB;                     /** port of the data backup channel */
    protected String ipAddressMDR;             /** IP address of the data recovery channel */
    protected int portMDR;                     /** port of the data recovery channel */


    /**
     * Constructor of the peer.
     * @param peerID identifier of the peer
     * @param ipAddressMC IP address of the control channel
     * @param portMC port of the control channel
     * @param ipAddressMDB IP address of the data backup channel
     * @param portMDB port of the data backup channel
     * @param ipAddressMDR IP address of the data recovery channel
     * @param portMDR port of the data recovery channel
     */
    public Protocol(int peerID,
                    String ipAddressMC, int portMC, 
                    String ipAddressMDB, int portMDB, 
                    String ipAddressMDR, int portMDR) {

        this.ipAddressMC = ipAddressMC;
        this.portMC = portMC;
        this.ipAddressMDB = ipAddressMDB;
        this.portMDB = portMDB;
        this.ipAddressMDR = ipAddressMDR;
        this.portMDR = portMDR;

        this.peerID = peerID;
        this.chunkManager = new ChunkManager(this.peerID);
        this.fileManager = new FileManager(this.peerID);
        this.protocolVersion = "";
    }


    /**
     * Method that retrieves the current protocol's version.
     * @return string with the protocol version
     */
    public String getVersion() {
        return this.protocolVersion;
    }

    /**
     * Sets the current protocol's version.
     * @param version the new protocol version
     */
    protected void setVersion(String version) {
        this.protocolVersion = version;
    }


    /**
     * Get method for the peer identifier.
     * @return identifier of the peer
     */
    public int getPeerID() {
        return this.peerID;
    }

    // --------------------------
    // Backup

    /**
     * Abstract function to be called by the initiator peer when a backup operation is to be done.
     * @param filepath path of the file
     * @param modificationDate modification date of the file
     * @param chunkNo chunk number
     * @param fileContent content of the file/chunk to be backed up
     * @param replicationDeg desired replication degree for the chunk
     */
    public abstract void initiateBackup(String filepath, String modificationDate, int chunkNo, byte[] fileContent, int replicationDeg);

    /**
     * Abstract function that tells other peers to backup a specific chunk (to be called by the initiator peer).
     * @param fileId identifier of the file
     * @param chunkNo chunk number
     * @param fileContent content of the file/chunk to be backed up
     * @param replicationDeg desired replication degree for the chunk
     */
    protected abstract void backupChunk(String fileId, int chunkNo, byte[] fileContent, int replicationDeg);

    /**
     * Abstract function that backs up a chunk, after a PUTCHUNK message is received.
     * @param message message received from the initiator peer (PUTCHUNK)
     */
    public abstract void handleBackup(Message message);

    /**
     * Abstract function to be called after a STORED message is received.
     * @param message message received from the peer that backed up the chunk
     */
    public abstract void stored(Message message);

    /**
     * Abstract function that tells other peers to delete the chunks of a file if the content of the chunks is outdated.
     * @param filepath path of the file
     * @param modificationDate modification date of the file
     */
    public abstract void deleteIfOutdated(String filepath, String modificationDate);

    // --------------------------
    // Restore

    /**
     * Abstract function to be called by the initiator peer when a restore operation is to be done.
     * @param filepath path of the file
     */
    public abstract void initiateRestore(String filepath);

    /**
     * Abstract function that sends a chunk back to the initiator peer, when a GETCHUNK message is received.
     * @param message message received from the initiator peer (GETCHUNK)
     */
    public abstract void sendChunk(Message message);

    /**
     * Abstract function that is called by the initiator peer when a CHUNK message is received.
     * @param message message received (CHUNK)
     */
    public abstract void receiveChunk(Message message);

    // --------------------------
    // Delete

    /**
     * Abstract function to be called by the initiator peer when a delete operation is to be done.
     * @param filepath path of the file
     */
    public abstract void initiateDelete(String filepath);

    /**
     * Abstract function to be called when a DELETE message is received.
     * @param message message received (DELETE)
     */
    public abstract void delete(Message message);

    /**
     * Abstract function to be called when a DELETED message is received (only used in protocol for enhancements).
     * @param message message received (DELETED)
     */
    public abstract void receiveDeleted(Message message);

    // --------------------------
    // Reclaim

    /**
     * Abstract function to be called by the initiator peer when a reclaim operation is to be done.
     * @param newMaximumStorageCapacity new maximum storage capacity for the peer
     */
    public abstract void reclaim(int newMaximumStorageCapacity);

    /**
     * Abstract function to be called when a REMOVED message is received.
     * @param message message received (REMOVED)
     */
    public abstract void removed(Message message);

    // --------------------------
    // General

    /**
     * Abstract function for send a greetings message to other peers (only used in protocol for enhancements).
     */
    public abstract void sendGreetings();

    /**
     * Abstract function for receiving and parsing a header (only used in protocol for enhancements).
     * @param header received header
     */
    public abstract void receivedHeader(Header header);

    // --------------------------
    // State

    /**
     * Abstract function to be called by the a peer when its current state is requested.
     * @return string containing information about the current state of the peer
     */
    public abstract String state();
}

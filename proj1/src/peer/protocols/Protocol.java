package peer.protocols;

import peer.ChunkManager;
import peer.FileManager;
import peer.messages.Header;
import peer.messages.Message;


public abstract class Protocol {
    protected final int TIMEOUT = 1000;
    protected final int CHUNK_SIZE = 64000;
    protected int peerID;                 // peer identifier
    protected ChunkManager chunkManager;     // chunk manager
    protected FileManager fileManager;       // current available disk space
    protected String protocolVersion;        // protocol version

    protected String ipAddressMC;
    protected int portMC;
    protected String ipAddressMDB; 
    protected int portMDB; 
    protected String ipAddressMDR; 
    protected int portMDR;

    //Constructor
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


    //Version handling
    public String getVersion() {
        return this.protocolVersion;
    }
    protected void setVersion(String version) { this.protocolVersion = version; }


    //Peer Id
    public int getPeerID() {
        return this.peerID;
    }

    // Backup
    public abstract void initiateBackup(String filePath, String modificationDate, int chunkNo, byte[] fileContent, int replicationDeg);
    protected abstract void backupChunk(String fileId, int chunkNo, byte[] fileContent, int replicationDeg);
    public abstract void handleBackup(Message message);
    public abstract void stored(Message message);
    public abstract void deleteIfOutdated(String filepath, String modificationDate);

    // Restore
    public abstract void initiateRestore(String filepath);
    public abstract void sendChunk(Message message);
    public abstract void receiveChunk(Message message);

    // Delete
    public abstract void initiateDelete(String filepath);
    public abstract void delete(Message message);
    public void receiveDeleted(Message message) {}

    // Reclaim
    public abstract void reclaim(int newMaximumStorageCapacity);
    public abstract void removed(Message message);

    // Header handling
    public void receivedHeader(Header header) {}

    // State
    public abstract String state();
}

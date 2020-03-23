package peer.protocols;

import peer.ChunkManager;
import peer.FileManager;
import peer.messages.Message;

import java.net.MulticastSocket;

public abstract class Protocol {
    protected MulticastSocket mCastControl;  // multicast socket to send control messages
    protected MulticastSocket mCastBackup;   // multicast socket to backup file chunk data
    protected MulticastSocket mCastRestore;  // multicast socket to restore file chunk data
    protected int peerID;                 // peer identifier
    protected ChunkManager chunkManager;     // chunk manager
    protected FileManager fileManager;       // current available disk space
    protected String protocolVersion;        // protocol version


    public Protocol(MulticastSocket mCastControl, MulticastSocket mCastBackup,
                    MulticastSocket mCastRestore, int peerID, String protocolVersion) {
        this.mCastControl = mCastControl;
        this.mCastBackup = mCastBackup;
        this.mCastRestore = mCastRestore;
        this.peerID = peerID;
        this.chunkManager = new ChunkManager(this.peerID);
        this.fileManager = new FileManager(this.peerID);
        this.protocolVersion = protocolVersion;
    }

    //Version handling
    public String getVersion() {
        return this.protocolVersion;
    }

    // Backup
    public abstract int initiateBackup(String fileId, int chunkNo, String fileContent, int replicationDeg);
    public abstract void handleBackup(Message message);
    public abstract void stored(Message message);

    // Restore
    public abstract void sendChunk(Message message);
    public abstract void receiveChunk(Message message);

    // Delete
    public abstract void delete(Message message);

    // Reclaim
    public abstract void removed(Message message);
}

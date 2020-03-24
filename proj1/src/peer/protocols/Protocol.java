package peer.protocols;

import peer.ChunkManager;
import peer.FileManager;
import peer.messages.Message;

import java.net.*;
import java.io.IOException;


public abstract class Protocol {
    protected MulticastSocket mCastControl;  // multicast socket to send control messages
    protected MulticastSocket mCastBackup;   // multicast socket to backup file chunk data
    protected MulticastSocket mCastRestore;  // multicast socket to restore file chunk data
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


    public Protocol(int peerID, String protocolVersion, 
                    String ipAddressMC, int portMC, 
                    String ipAddressMDB, int portMDB, 
                    String ipAddressMDR, int portMDR) throws IOException {

        this.mCastControl = new MulticastSocket(portMC);
        this.mCastControl.joinGroup(InetAddress.getByName(ipAddressMC));
        this.mCastControl.setTimeToLive(1);
        System.out.println("MC channel up!");

        this.mCastBackup = new MulticastSocket(portMDB);
        this.mCastBackup.joinGroup(InetAddress.getByName(ipAddressMDB));
        this.mCastBackup.setTimeToLive(1);
        System.out.println("MDB channel up!");

        this.mCastRestore = new MulticastSocket(portMDR);
        this.mCastRestore.joinGroup(InetAddress.getByName(ipAddressMDR));
        this.mCastRestore.setTimeToLive(1);
        System.out.println("MDR channel up!");

        this.ipAddressMC = ipAddressMC;
        this.portMC = portMC;
        this.ipAddressMDB = ipAddressMDB;
        this.portMDB = portMDB;
        this.ipAddressMDR = ipAddressMDR;
        this.portMDR = portMDR;

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

    public MulticastSocket getMCastControl() {
        return mCastControl;
    }

    public MulticastSocket getMCastBackup() {
        return mCastBackup;
    }
    
    public MulticastSocket getMCastRestore() {
        return mCastRestore;
    }    
}

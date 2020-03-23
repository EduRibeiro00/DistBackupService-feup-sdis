package peer;
import peer.messages.Message;
import peer.messages.MessageHandler;
import peer.messages.MessageType;
import peer.protocols.Protocol;

import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;


public class Peer {
    private MulticastSocket mCastControl;                   // multicast socket to send control messages
    private MulticastSocket mCastBackup;                    // multicast socket to backup file chunk data
    private MulticastSocket mCastRestore;                   // multicast socket to restore file chunk data
    private String peerID;                                  // identifier of the peer
    private String protocolVersion;                         // protocol version that is being used

    private Protocol protocol;                              // protocol responsible for
    private MessageHandler messageHandler;                  // dispatcher of messages received
    private ChunkManager chunkManager;                      // chunk manager
    private int availableDiskSpace;                         // current available disk space

    private final static int N_THREADS_PER_CHANNEL = 10;    // number of threads ready for processing packets in each channel
    private final static int TIMEOUT = 2000;                // timeout value
    private final static String PEER_DIR = "peers/";        // constant that denotes the name of the peer directory
    private final static int BUFFER_SIZE_CONTROL = 500;     // buffer size for messages received in the control socket
    private final static int BUFFER_SIZE = 64500;           // buffer size for messages received in the control socket




    /**
     * Constructor of the peer
     * @param ipAddressMC IP Address of the MC channel
     * @param portMC Port of the MC channel
     * @param ipAddressMDB IP Address of the MDB channel
     * @param portMDB Port of the MDB channel
     * @param ipAddressMDR IP Address of the MDR channel
     * @param portMDR Port of the MDR channel
     * @param protocolVersion Protocol version of the peer
     * @param peerID Identifier of the peer
     */
    public Peer(String ipAddressMC, int portMC, String ipAddressMDB, int portMDB, String ipAddressMDR, int portMDR, String protocolVersion, int peerID) throws IOException {
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

        this.protocolVersion = protocolVersion;
        this.peerID = String.valueOf(peerID);

        this.chunkManager = new ChunkManager(String.valueOf(peerID));
        System.out.println("Started chunk manager...");

        this.availableDiskSpace = 6400000; // TODO: confirmar que e este o valor

        this.initReceivingThreads();
    }

    /**
     * Method that will create and start the threads responsible for receiving and processing messages
     */
    private void initReceivingThreads() {
        ReceiveThread controlThread = new ReceiveThread(this.messageHandler, this.mCastControl, BUFFER_SIZE_CONTROL, N_THREADS_PER_CHANNEL);
        ReceiveThread backupThread = new ReceiveThread(this.messageHandler, this.mCastBackup, BUFFER_SIZE, N_THREADS_PER_CHANNEL);
        ReceiveThread restoreThread = new ReceiveThread(this.messageHandler, this.mCastRestore, BUFFER_SIZE, N_THREADS_PER_CHANNEL);

        new Thread(controlThread).start();
        new Thread(backupThread).start();
        new Thread(restoreThread).start();
    }

    /**
     * Method that will create a directory for the peer, if it doesn't exist already
     * @param path Path of the folder to be created
     */
    private void initDirectory(String path) {

    }


    /**
     * Retrieves the protocol version.
     * @return
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }


    /**
     * Backup request.
     * Sends a backup message for peer-peer communication
     * @param version the version of the protocol to be used
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unusued)
     * @param fileContent the body of the message
     * @param replicationDeg the desired replication degree of the file's chunk (may be unused)
     */
    public int backupChunk(String version, String fileId, int chunkNo, String fileContent, int replicationDeg) {
        Message msg = null;
        try {
            msg = new Message(version, MessageType.PUTCHUNK, this.peerID, fileId, chunkNo, replicationDeg, fileContent);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return 0;
        }

        for (int i = 0; i < 5 && this.chunkManager.getReplicationDegree(fileId, chunkNo) < replicationDeg; i++) {
            try {
                msg.send(this.mCastBackup);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return this.chunkManager.getReplicationDegree(fileId, chunkNo);
    }
}


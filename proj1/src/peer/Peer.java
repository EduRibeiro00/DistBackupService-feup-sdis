package peer;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Peer implements RemoteInterface {
    private MulticastSocket mcast_control;  // multicast socket to send control messages
    private MulticastSocket mcast_backup;   // multicast socket to backup file chunk data
    private MulticastSocket mcast_restore;  // multicast socket to restore file chunk data
    private int peerID;                     // identifier of the peer
    private String protocolVersion;         // protocol version that is being used


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
        this.mcast_control = new MulticastSocket(portMC);
        this.mcast_control.joinGroup(InetAddress.getByName(ipAddressMC));
        this.mcast_control.setTimeToLive(1);

        this.mcast_backup = new MulticastSocket(portMDB);
        this.mcast_backup.joinGroup(InetAddress.getByName(ipAddressMDB));
        this.mcast_backup.setTimeToLive(1);

        this.mcast_restore = new MulticastSocket(portMDR);
        this.mcast_restore.joinGroup(InetAddress.getByName(ipAddressMDR));
        this.mcast_restore.setTimeToLive(1);

        this.protocolVersion = protocolVersion;
        this.peerID = peerID;
    }


    /**
     * Method to process backup requests received by a client
     * @param testString
     * @return
     */
    @Override
    public String backup(String testString) {
        // TODO
        return null;
    }


    @Override
    public String delete(String testString) {
        return null;
    }

    @Override
    public String restore(String testString) {
        return null;
    }

    @Override
    public String reclaim(String testString) {
        return null;
    }

    @Override
    public String state(String testString) {
        return null;
    }


    /**
     * Sends a backup message for peer-peer communication
     * @param version the version of the protocol to be used
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param repDeg the desired replication degree of the file's chunk (may be unused)
     * @param body the body of the message
     */
    private void sendBackupMessage(String version, String senderId, String fileId, int chunkNo, int repDeg, byte[] body) {
        Message msg = new Message(version, MessageType.PUTCHUNK, senderId, fileId, chunkNo, repDeg, body);
        //msg.send();
    }
}


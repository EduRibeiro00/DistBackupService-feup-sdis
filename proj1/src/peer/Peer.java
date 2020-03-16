package peer;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


public class Peer implements RemoteInterface {
    private MulticastSocket mcast_control;  // multicast socket to send control messages
    private MulticastSocket mcast_backup;   // multicast socket to backup file chunk data
    private MulticastSocket mcast_restore;  // multicast socket to restore file chunk data
    private String peerID;                     // identifier of the peer
    private String protocolVersion;         // protocol version that is being used
    private final static int TIMEOUT = 2000;

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
        this.peerID = String.valueOf(peerID);
    }



    /**
     * Method to process delete requests sent by a client
     * @param testString
     * @return
     */
    @Override
    public String delete(String testString) {
        return null;
    }


    /**
     * Method to process restore requests sent by a client
     * @param testString
     * @return
     */
    @Override
    public String restore(String testString) {
        return null;
    }


    /**
     * Method to process reclaim requests sent by a client
     * @param testString
     * @return
     */
    @Override
    public String reclaim(String testString) {
        return null;
    }


    /**
     * Method to process state requests sent by a client
     * @param testString
     * @return
     */
    @Override
    public String state(String testString) {
        return null;
    }


    /**
     * Backup request.
     * Sends a backup message for peer-peer communication
     * @param version the version of the protocol to be used
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param fileContent the body of the message
     * @param replicationDeg the desired replication degree of the file's chunk (may be unused)
     */
    @Override
    public int backup(String version, String fileId, int chunkNo, String fileContent, int replicationDeg) {
        List<String> replicationIDs = new ArrayList<>();
        Message msg;
        for (int i = 0; i < 5 && replicationIDs.size() < replicationDeg; i++) {
            try {
                msg = new Message(version, MessageType.PUTCHUNK, this.peerID, fileId, chunkNo, replicationDeg, fileContent);
                msg.send(mcast_backup);
            } catch (NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
                return replicationIDs.size();
            }

            try {
                Header msgHeader = msg.getHeader();
                int timeout = (int) (TIMEOUT * Math.pow(2, i));
                mcast_control.setSoTimeout(timeout);
                Message receivedMsg = new Message();

                while (true) {
                    try {
                        receivedMsg.receiveControl(mcast_control);
                        Header receivedMsgHeader = receivedMsg.getHeader();
                        if(receivedMsgHeader.getMessageType() == MessageType.STORED
                                && receivedMsgHeader.getFileId().equals(msgHeader.getFileId())
                                && receivedMsgHeader.getChunkNo() == msgHeader.getChunkNo()
                                && !replicationIDs.contains(receivedMsgHeader.getSenderId())) {

                            replicationIDs.add(receivedMsgHeader.getSenderId());
                            mcast_control.setSoTimeout(timeout);
                        }
                    } catch (Exception ignored) { }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return replicationIDs.size();
    }
}


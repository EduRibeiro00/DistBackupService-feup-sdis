package peer;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;


public class Peer implements RemoteInterface {
    private static MulticastSocket mcast_control;   // multicast socket to send control messages
    private static MulticastSocket mcast_backup;    // multicast socket to backup file chunk data
    private static MulticastSocket mcast_restore;   // multicast socket to restore file chunk data
    final static int TIMEOUT = 10000;


    @Override
    public String test(String testString) {
        String str = "Ya, recebi. A string foi: " + testString;
        return str;
    }

    /**
     * Sends a backup message for peer-peer communication
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param fileContent the body of the message
     * @param replicationDeg the desired replication degree of the message
     * @throws RemoteException
     */
    @Override
    public int backup(String fileId, int chunkNo, byte[] fileContent, int replicationDeg) throws RemoteException {
        int replication = 0;
        Message msg = new Message("1.0", MessageType.PUTCHUNK, this.peerID, fileId, chunkNo, replicationDeg, fileContent);
        try {
            msg.send(mcast_backup);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return replication;
        }

        try {
            mcast_control.setSoTimeout(TIMEOUT);

            DatagramPacket pkt = new DatagramPacket(new byte[1000], 1000);
            while (true) {
                mcast_control.receive(pkt);
                try {
                    Message receivedMsg = new Message(pkt.getData());
                    if(receivedMsg.header.messageType == MessageType.STORED
                            && receivedMsg.header.fileId.equals(msg.header.fileId)) {
                        replication++;
                        mcast_control.setSoTimeout(TIMEOUT);
                    }
                } catch (Exception ignored) { }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return replication;
    }


    /**
     * Sends a backup message for peer-peer communication
     * @param version the version of the protocol to be used
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param body the body of the message
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private void sendBackupMessage(String version, String senderId, String fileId, int chunkNo, int repDeg, String body) throws NoSuchAlgorithmException, IOException {
        Message msg = new Message(version, MessageType.PUTCHUNK, senderId, fileId, chunkNo, repDeg, body);
        msg.send(mcast_backup);
    }
}


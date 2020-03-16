package peer;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


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
        List<String> replicationIDs = new ArrayList<>();

        Message msg = new Message("1.0", MessageType.PUTCHUNK, this.peerID, fileId, chunkNo, replicationDeg, fileContent);
        try {
            msg.send(mcast_backup);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return replicationIDs.size();
        }

        try {
            mcast_control.setSoTimeout(TIMEOUT);

            DatagramPacket pkt = new DatagramPacket(new byte[1000], 1000);
            while (true) {
                mcast_control.receive(pkt);
                try {
                    Message receivedMsg = new Message(pkt.getData());
                    if(receivedMsg.header.messageType == MessageType.STORED
                            && receivedMsg.header.fileId.equals(msg.header.fileId)
                            && !replicationIDs.contains(receivedMsg.header.senderId)) {

                        replicationIDs.add(receivedMsg.header.senderId);
                        mcast_control.setSoTimeout(TIMEOUT);
                    }
                } catch (Exception ignored) { }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return replicationIDs.size();
    }
}


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
    final static int TIMEOUT = 2000;




    @Override
    public String test(String testString) {
        return String.format("Ya, recebi. A string foi: %s", testString);
    }

    /**
     * Sends a backup message for peer-peer communication
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param fileContent the body of the message
     * @param replicationDeg the desired replication degree of the message
     */
    @Override
    public int backup(String fileId, int chunkNo, byte[] fileContent, int replicationDeg) {
        List<String> replicationIDs = new ArrayList<>();

        for (int i = 0; i < 5 && replicationIDs.size() < replicationDeg; i++) {
            Message msg = new Message("1.0", MessageType.PUTCHUNK, this.peerID, fileId, chunkNo, replicationDeg, fileContent);
            try {
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


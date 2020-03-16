package peer;
import java.net.*;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Peer implements RemoteInterface {
    private static MulticastSocket mcast_control;   // multicast socket to send control messages
    private static MulticastSocket mcast_backup;    // multicast socket to backup file chunk data
    private static MulticastSocket mcast_restore;   // multicast socket to restore file chunk data
    private static String[] messageTypeList = {"PUTCHUNK", "STORED", "GETCHUNK", "CHUNK", "DELETE", "REMOVED"};


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
     */
    @Override
    public int backup(String fileId, int chunkNo, byte[] fileContent, int replicationDeg) throws RemoteException {
        Message msg = new Message("1.0", MessageType.PUTCHUNK, this.id, fileId, chunkNo, replicationDeg, fileContent);
//        msg.send(mcast_)

        return 0;
    }


    /**
     * Sends a backup message for peer-peer communication
     * @param version the version of the protocol to be used
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param body the body of the message
     */
    private void sendBackupMessage(String version, String senderId, String fileId, int chunkNo, int repDeg, byte[] body) {
        Message msg = new Message(version, MessageType.PUTCHUNK, senderId, fileId, chunkNo, repDeg, body);
//        msg.send();
    }
}


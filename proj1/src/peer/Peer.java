package peer;
import java.io.IOException;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;


public class Peer implements RemoteInterface {
    private static MulticastSocket mcast_control;   // multicast socket to send control messages
    private static MulticastSocket mcast_backup;    // multicast socket to backup file chunk data
    private static MulticastSocket mcast_restore;   // multicast socket to restore file chunk data

    @Override
    public String test(String testString) {
        String str = "Ya, recebi. A string foi: " + testString;
        return str;
    }

    /**
     * Sends a backup message for peer-peer communication
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param RepDeg the desired replication degree of the file's chunk (may be unused)
     * @param body the body of the message
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private void sendBackupMessage(String version, String senderId, String fileId, int chunkNo, int repDeg, String body) throws NoSuchAlgorithmException, IOException {
        Message msg = new Message(version, MessageType.PUTCHUNK, senderId, fileId, chunkNo, repDeg, body);
        msg.send(mcast_backup);
    }
}


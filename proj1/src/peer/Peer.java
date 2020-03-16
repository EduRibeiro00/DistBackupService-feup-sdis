package peer;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Peer {
    private static MulticastSocket mcast_control;   // multicast socket to send control messages
    private static MulticastSocket mcast_backup;    // multicast socket to backup file chunk data
    private static MulticastSocket mcast_restore;   // multicast socket to restore file chunk data
    private static String[] MessageTypeList = {"PUTCHUNK", "STORED", "GETCHUNK", "CHUNK", "DELETE", "REMOVED"};

    /**
     * Main function
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // check arguments
        if (args.length != 1) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        String remoteObjectName = args[0];
        RemoteObject srvObj = new RemoteObject();

        try {
            RemoteInterface remoteObject = (RemoteInterface) UnicastRemoteObject.exportObject(srvObj, 0);
            Registry rmiReg = LocateRegistry.getRegistry();
            rmiReg.bind(remoteObjectName, remoteObject);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a backup message for peer-peer communication
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param RepDeg the desired replication degree of the file's chunk (may be unused)
     */
    private void sendBackupMessage(String version, String senderId, String fileId, int chunkNo, int repDeg, byte[] body) {
        Message msg = new Message(version, MessageType.PUTCHUNK, senderId, fileId, chunkNo, repDeg, body);
        //msg.send();
    }
}


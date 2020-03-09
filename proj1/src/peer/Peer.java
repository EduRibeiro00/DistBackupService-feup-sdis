package peer;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

enum MessageType {
    PUTCHUNK,
    STORED,
    GETCHUNK,
    CHUNK,
    DELETE,
    REMOVED
}

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
     * Creates a message header for peer-peer communication
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param RepDeg the desired replication degree of the file's chunk (may be unused)
     * @return the message header in a string
     */
    private String createMessageHeader(String version, MessageType msgType, String senderId, byte[] fileId, int chunkNo, int RepDeg) {
        String fileIdStr = encodeFileId(fileId);
        String msgTypeStr = MessageTypeList[msgType.ordinal()];

        String header = version + " " + msgTypeStr + " " + senderId + " " + fileId;
        switch(msgType) {
            case PUTCHUNK:
                header += " " + Integer.toString(chunkNo) + " " + Integer.toString(RepDeg);
                break;
            case STORED:
            case GETCHUNK:
            case CHUNK:
            case REMOVED:
                header += " " + Integer.toString(chunkNo);
                break;
        }
        header += "\r\n\r\n"; // Warning - In case we decided to use "multiple headers inside of the same header" the last <CRLF> needs to be removed
        return header;
    }

    /**
     * Encodes the file ID so that it can be sent in a message header
     * @param fileId the file identifier in the backup service
     * @return the enconded file ID in a string
     */
    private String encodeFileId(byte[] fileId) {
        StringBuilder result = new StringBuilder();
        for(byte bt : fileId) {
            int decimal = (int) bt & 0xff;  // bytes widen to int, need mask as to prevent sign extension
            String hex = Integer.toHexString(decimal);
            result.append(hex);
        }
        return result.toString();
    }

    /**
     * Hashed a string using the SHA-256 cryptographic function
     * @param str the string to be hashed
     * @return an array of bytes resultant of the hash
     * @throws NoSuchAlgorithmException
     */
    private byte[] getSHA256(String str) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
        return encodedhash;
    }
}

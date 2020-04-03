package peer;

import link.RemoteInterface;
import peer.messages.MessageHandler;
import peer.protocols.Protocol;
import peer.protocols.Protocol1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that makes the connection between the peer and the testing client
 */
public class Peer implements RemoteInterface {
    private final static int N_THREADS_PER_CHANNEL = 10;    // number of threads ready for processing packets in each channel
    private final static int BUFFER_SIZE_CONTROL = 2000;     // buffer size for messages received in the control socket
    private final static int BUFFER_SIZE = 64000;           // buffer size for messages received in the control socket

    private Protocol protocol;  // protocol responsible for the peer behaviours


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
    public Peer(String ipAddressMC, int portMC, String ipAddressMDB, int portMDB, String ipAddressMDR, int portMDR, String protocolVersion, int peerID) throws Exception {

        switch (protocolVersion) {
            case "1.0":
                this.protocol = new Protocol1(peerID, ipAddressMC, portMC, ipAddressMDB, portMDB, ipAddressMDR, portMDR);
                break;
            default:
                throw new Exception(String.format("Version %s not available", protocolVersion));
        }

        System.out.println("Started protocol...");

        MessageHandler messageHandler = new MessageHandler(this.protocol);

        ReceiverThread controlThread = new ReceiverThread(messageHandler, ipAddressMC, portMC, BUFFER_SIZE_CONTROL, N_THREADS_PER_CHANNEL);
        ReceiverThread backupThread = new ReceiverThread(messageHandler, ipAddressMDB, portMDB,BUFFER_SIZE, N_THREADS_PER_CHANNEL);
        ReceiverThread restoreThread = new ReceiverThread(messageHandler, ipAddressMDR, portMDR, BUFFER_SIZE, N_THREADS_PER_CHANNEL);

        new Thread(controlThread).start();
        new Thread(backupThread).start();
        new Thread(restoreThread).start();

        System.out.println("Started all threads...");
    }


    /**
     * Implementation of the backup request.
     * @param filepath filepath of the file we want to backup
     * @param replicationDegree desired replication factor for the file's chunks
     * @throws RemoteException
     */
    @Override
    public void backup(String filepath, int replicationDegree) {
        if (filepath == null || replicationDegree < 1 || replicationDegree > 9) {
            throw new IllegalArgumentException("Invalid arguments for backup!");
        }

        FileReader fileReader;
        try {
            fileReader = new FileReader(filepath);
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
            return;
        }

        char[] fileContent = new char[64000];
        int chunkNo = 0;
        int minReplication = -1;

        try {
            int charsRead, lastRead = 0;
            while ((charsRead = fileReader.read(fileContent, 0, 64000)) != -1) {
                int replication = this.protocol.initiateBackup(filepath,
                        chunkNo,
                        new String(fileContent, 0, charsRead),
                        replicationDegree);

                if (minReplication == -1 || minReplication > replication) {
                    minReplication = replication;
                }

                chunkNo++;
                lastRead = charsRead;
            }
            if(lastRead == 64000) {
                this.protocol.initiateBackup(filepath, chunkNo, "", replicationDegree);
            }
        } catch (IOException e) {
            System.err.println("Error while reading file " + filepath);
        }

        System.out.println("Replication degree achieved: " + (minReplication == -1 ? 0 : minReplication));
    }

    /**
     * Implementation of the delete request.
     * @param filepath filepath of the file we want to delete
     * @throws RemoteException
     */
    @Override
    public void delete(String filepath) {
        this.protocol.initiateDelete(filepath);
    }


    /**
     * Implementation of the restore request.
     * @param filepath filepath of the file we want to restore
     * @throws RemoteException
     */
    @Override
    public void restore(String filepath) {

    }


    /**
     * Implementation of the reclaim request.
     * @param diskSpace maximum of disk space we want to reclaim
     * @throws RemoteException
     */
    @Override
    public void reclaim(int diskSpace) {

    }


    /**
     * Implementation of the state request.
     * @return String with peer state
     * @throws RemoteException
     */
    @Override
    public String state() {
        return null;
    }
}

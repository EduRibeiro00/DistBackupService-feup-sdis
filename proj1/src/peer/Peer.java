package peer;

import link.RemoteInterface;
import peer.messages.MessageHandler;
import peer.protocols.Protocol;
import peer.protocols.Protocol1;

import java.io.*;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Class that makes the connection between the peer and the testing client
 */
public class Peer implements RemoteInterface {
    private final static int N_THREADS_PER_CHANNEL = 10;    // number of threads ready for processing packets in each channel
    private final static int BUFFER_SIZE_CONTROL = 2000;     // buffer size for messages received in the control socket
    private final static int BUFFER_SIZE = 64500;           // buffer size for messages received in the control socket
    public static final int CHUNK_SIZE = 64000;


    private Protocol protocol;  // protocol responsible for the peer behaviours
    private ExecutorService service; // ExecutorService responsible for threads

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

        this.service = Executors.newFixedThreadPool(N_THREADS_PER_CHANNEL);

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
        this.service.execute(() -> {
            if (filepath == null || replicationDegree < 1 || replicationDegree > 9) {
                throw new IllegalArgumentException("Invalid arguments for backup!");
            }

            File file = new File(filepath);
            if (!file.exists()) {
                System.err.println("File not found");
                return;
            }

            byte[] totalFileContent;
            try {
                totalFileContent = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                System.err.println("Error while trying to read from file");
                return;
            }


            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            String modificationDate = sdf.format(file.lastModified());

            this.protocol.deleteIfOutdated(filepath, modificationDate);

            int chunkNo = 0;
            int minReplication = -1;

            int currentIndex = 0, lastIndex = CHUNK_SIZE;

            for (; currentIndex < totalFileContent.length; lastIndex += CHUNK_SIZE) {
                if (lastIndex > totalFileContent.length) {
                    lastIndex = totalFileContent.length;
                }

                byte[] chunkContent = Arrays.copyOfRange(totalFileContent, currentIndex, lastIndex);
                int replication = this.protocol.initiateBackup(filepath,
                        modificationDate,
                        chunkNo,
                        chunkContent,
                        replicationDegree);

                if (minReplication == -1 || minReplication > replication) {
                    minReplication = replication;
                }

                chunkNo++;
                currentIndex = lastIndex;
            }

            if (totalFileContent.length % CHUNK_SIZE == 0) {
                this.protocol.initiateBackup(filepath, modificationDate, chunkNo, new byte[0], replicationDegree);
            }

            System.out.println("Replication degree achieved: " + (minReplication == -1 ? 0 : minReplication));
        });
    }

    /**
     * Implementation of the delete request.
     * @param filepath filepath of the file we want to delete
     * @throws RemoteException
     */
    @Override
    public void delete(String filepath) {
        this.service.execute(() -> this.protocol.initiateDelete(filepath));
    }


    /**
     * Implementation of the restore request.
     * @param filepath filepath of the file we want to restore
     * @throws RemoteException
     */
    @Override
    public void restore(String filepath) {
        this.service.execute(() -> this.protocol.initiateRestore(filepath));
    }


    /**
     * Implementation of the reclaim request.
     * @param diskSpace maximum of disk space we want to reclaim
     * @throws RemoteException
     */
    @Override
    public void reclaim(int diskSpace) {
        this.service.execute(() -> this.protocol.reclaim(diskSpace));
    }


    /**
     * Implementation of the state request.
     * @return String with peer state
     */
    @Override
    public String state() {
        StringBuilder stateInformation = new StringBuilder();
        stateInformation.append("STATE INFORMATION\n----------------\n");
        stateInformation.append(this.protocol.state());
        stateInformation.append("----------------\n");
        return stateInformation.toString();
    }
}

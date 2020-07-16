package peer;

import link.RemoteInterface;
import peer.jsse.ReceiverThread;
import peer.jsse.SenderThread;
import peer.messages.MessageHandler;
import peer.protocols.Protocol;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Class that makes the connection between the peer and the testing client.
 */
public class Peer implements RemoteInterface {
    private final static int N_THREADS = 30;               /** number of threads ready for processing packets in each channel */
    public static final int CHUNK_SIZE = 64000;            /** chunk size constant */

    private final Protocol protocol;           /** protocol responsible for the peer behaviours */
    private final ExecutorService service;     /** ExecutorService responsible for threads */

    public Peer(String ipAddress, int portMC, int portMDB, int portMDR, int portChord,
                String protocol, String serverKeys, String clientKeys,  String trustStore, String password,
                String initIpAddress, int initPort) {

        this.protocol = new Protocol(ipAddress, portMC, portMDB, portMDR, portChord);

        System.out.println("Started protocol...");

        MessageHandler messageHandler = new MessageHandler(this.protocol);

        this.service = Executors.newFixedThreadPool(N_THREADS);

        try {
            ReceiverThread receiverThread = new ReceiverThread(messageHandler, protocol, serverKeys, trustStore, password, N_THREADS);
            receiverThread.addServer(ipAddress, portMC);
            receiverThread.addServer(ipAddress, portMDB);
            receiverThread.addServer(ipAddress, portMDR);
            receiverThread.addServer(ipAddress, portChord);

            //starting the receiver thread
            new Thread(receiverThread).start();

        } catch (Exception e) {
            System.err.println("Could not initiate receiver thread");
            e.printStackTrace();
        }

        SenderThread.setSenderId(this.protocol.getPeerID());
        SenderThread.setProtocol(protocol);
        SenderThread.setClientKeys(clientKeys);
        SenderThread.setTrustStore(trustStore);
        SenderThread.setPassword(password);

        System.out.println("Started all threads...");


        this.protocol.getChord().createOrJoin(initIpAddress, initPort);

        System.out.println("Starting chord ring creation/joining...");
    }


    /**
     * Implementation of the backup request.
     * @param filepath filepath of the file we want to backup
     * @param replicationDegree desired replication factor for the file's chunks
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

            AsynchronousFileChannel fileChannel;

            try {
                fileChannel = AsynchronousFileChannel.open(Paths.get(filepath), StandardOpenOption.READ);
            } catch (IOException e) {
                System.err.println("Error while trying to read from file");
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            String modificationDate = sdf.format(file.lastModified());

            this.protocol.deleteIfOutdated(filepath, modificationDate);

            int fileSize = (int) file.length();
            int numChunks = fileSize / CHUNK_SIZE;
            if (fileSize % CHUNK_SIZE != 0)
                numChunks++;

            if (numChunks == 0)
                return;

            final String encodedFileId = protocol.startFileBackup(
                        filepath, modificationDate);

            for (int chunkNo = 0; chunkNo < numChunks; chunkNo++) {
                ByteBuffer buf = ByteBuffer.allocate(CHUNK_SIZE);
                long position = chunkNo * CHUNK_SIZE;
                final int chunkNoFinal = chunkNo;

                fileChannel.read(buf, position, buf, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        attachment.flip();
                        byte[] data = new byte[attachment.limit()];
                        attachment.get(data);
                        protocol.initiateBackup(
                                encodedFileId,
                                chunkNoFinal,
                                data,
                                replicationDegree);

                        attachment.clear();
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {

                    }
                });
            }

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
     * Tells the peer to exit the distributed system.
     */
    @Override
    public void exit() {
        this.service.execute(this.protocol::exit);
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

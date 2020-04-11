package peer.protocols;

import peer.FileDeleter;
import peer.messages.Header;
import peer.messages.Message;
import peer.messages.MessageType;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Class that represents the protocol with enhancements
 */
public class Protocol2 extends Protocol1 {
    /**
     * Constructor of the protocol.
     * @param peerID identifier of the peer
     * @param ipAddressMC IP address of the control channel
     * @param portMC port of the control channel
     * @param ipAddressMDB IP address of the data backup channel
     * @param portMDB port of the data backup channel
     * @param ipAddressMDR IP address of the data recovery channel
     * @param portMDR port of the data recovery channel
     */
    public Protocol2(int peerID, String ipAddressMC, int portMC, String ipAddressMDB, int portMDB, String ipAddressMDR, int portMDR) {
        super(peerID, ipAddressMC, portMC, ipAddressMDB, portMDB, ipAddressMDR, portMDR);
        this.setVersion("1.1");
    }

    /**
     * Method that tells other peers to backup a specific chunk (to be called by the initiator peer).
     * @param fileId identifier of the file
     * @param chunkNo chunk number
     * @param fileContent content of the file/chunk to be backed up
     * @param replicationDeg desired replication degree for the chunk
     */
    @Override
    protected void backupChunk(String fileId, int chunkNo, byte[] fileContent, int replicationDeg) {
        this.chunkManager.removeFileDeletion(fileId);
        super.backupChunk(fileId, chunkNo, fileContent, replicationDeg);
    }


    /**
     * Method that backs up a chunk, after a PUTCHUNK message is received.
     * @param message message received from the initiator peer (PUTCHUNK)
     */
    @Override
    public void handleBackup(Message message) {
        Header header = message.getHeader();

        // TODO: confirmar isto
        this.chunkManager.removeFileDeletion(header.getFileId());

        this.chunkManager.setDesiredReplication(header.getFileId(), header.getReplicationDeg());
        this.fileManager.setMaxChunkNo(header.getFileId(), header.getChunkNo());

        try {
            if(this.chunkManager.getPerceivedReplication(header.getFileId(), header.getChunkNo()) <
                    this.chunkManager.getDesiredReplication(header.getFileId())) {

                if (!this.fileManager.storeChunk(header.getFileId(), header.getChunkNo(), message.getBody())) {
                    return;
                }

                this.chunkManager.addChunkReplication(header.getFileId(), header.getChunkNo(), this.peerID);
            }

            if (this.fileManager.isChunkStored(header.getFileId(), header.getChunkNo())) {
                executor.schedule(() -> {
                    try {
                        new Message(this.protocolVersion,
                                MessageType.STORED,
                                this.peerID,
                                header.getFileId(),
                                header.getChunkNo()
                        ).send(this.ipAddressMC, this.portMC);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, new Random().nextInt(401), TimeUnit.MILLISECONDS);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Method that sends a chunk back to the initiator peer, when a GETCHUNK message is received.
     * @param message message received from the initiator peer (GETCHUNK)
     */
    @Override
    public void sendChunk(Message message) {
        Header header = message.getHeader();
        String fileId = header.getFileId();
        int chunkNo = header.getChunkNo();

        // if the peer has that chunk saved
        if (this.fileManager.isChunkStored(fileId, chunkNo)) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(CHUNK_SIZE);
            Future<Integer> future;
            try {
                future = this.fileManager.getChunk(byteBuffer, fileId, chunkNo);

            } catch (IOException e) {
                e.printStackTrace(); // TODO: change this
                return;
            }

            MulticastSocket mCastSkt;

            long waitTime = new Random().nextInt(401);
            try {
                mCastSkt = new MulticastSocket(this.portMDR);
                mCastSkt.joinGroup(InetAddress.getByName(this.ipAddressMDR));
                mCastSkt.setTimeToLive(1);
                mCastSkt.setSoTimeout((int) waitTime);
            } catch (IOException e) {
                e.printStackTrace(); // TODO: change this
                return;
            }

            byte[] buffer = new byte[64500];
            DatagramPacket packet = new DatagramPacket(buffer, 64500);
            try {
                while (waitTime > 0) {
                    long before = System.currentTimeMillis();
                    mCastSkt.receive(packet);
                    waitTime -= System.currentTimeMillis() - before;

                    Message msg = new Message(packet.getData());
                    if (msg.getHeader().getMessageType() == MessageType.CHUNK &&
                            msg.getHeader().getFileId().equals(fileId) &&
                            msg.getHeader().getChunkNo() == chunkNo) {
                        return;
                    }
                    mCastSkt.setSoTimeout((int) waitTime);
                }
            } catch (IOException ignore) {
            }

            // send message with chunk
            switch (header.getVersion()) {
                case "1.1":
                    try {
                        ServerSocket serverSocket = new ServerSocket(0);
                        int chunkPort = serverSocket.getLocalPort();

                        new Message(this.protocolVersion, )

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;

                case "1.0":

                    try {
                        future.get();
                        byteBuffer.flip();
                        byte[] chunkContent = new byte[byteBuffer.limit()];
                        byteBuffer.get(chunkContent);
                        new Message(this.protocolVersion, MessageType.CHUNK, this.peerID, fileId, chunkNo, chunkContent).send(
                                this.ipAddressMDR, this.portMDR
                        );
                        byteBuffer.clear();
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        System.err.println("Error sending chunk message");
                        e.printStackTrace();
                    }

                    break;
            }
        }

    }


    /**
     * Method to be called by the initiator peer when a delete operation is to be done.
     * @param filepath path of the file
     */
    @Override
    public void initiateDelete(String filepath) {
        String fileId = this.fileManager.getHashForFile(filepath);
        if(fileId == null) {
            System.err.println("Unknown filepath given");
            return;
        }

        Message msg = new Message(this.protocolVersion, MessageType.DELETE, this.peerID, fileId);

        ArrayList<Integer> storers = this.chunkManager.getFileStorers(fileId, this.fileManager.getMaxChunkNo(fileId), this.peerID);

        for(Integer storer : storers) {
            this.chunkManager.addToFileDeleter(storer, msg, this.ipAddressMC, this.portMC);
        }

        sendDeleteMsgLoop(msg, 0, filepath, fileId);
    }


    /**
     * Method to be called when a DELETE message is received.
     * @param message message received (DELETE)
     */
    @Override
    public void delete(Message message) {
        String fileId = message.getHeader().getFileId();

        for (int i = 0; i <= this.fileManager.getMaxChunkNo(fileId); i++) {
            this.chunkManager.deletePerceivedReplication(fileId, i);

            try {
                this.fileManager.removeChunk(fileId, i);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println(this.fileManager.getFileChunks(fileId).size() == 0 ? "Successfully deleted all chunks" : "Failed to delete all chunks");

        this.chunkManager.deleteDesiredReplication(fileId);
        this.fileManager.removeFile(fileId);

        Message msg = new Message(this.protocolVersion, MessageType.DELETED, this.peerID, fileId);

        executor.schedule(() -> {
                    try {
                        msg.send(this.ipAddressMC, this.portMC);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                },
                new Random().nextInt(401),
                TimeUnit.MILLISECONDS);
    }


    /**
     * Method to be called when a DELETED message is received (only used in protocol for enhancements).
     * @param message message received (DELETED)
     */
    @Override
    public void receiveDeleted(Message message) {
        Header header = message.getHeader();
        this.chunkManager.removeFromFileDeleter(header.getSenderId(), header.getFileId());
    }

    /**
     * Method for receiving and parsing a header (only used in protocol for enhancements).
     * @param header received header
     */
    @Override
    public void receivedHeader(Header header) {
        this.chunkManager.getFileDeleter(header.getSenderId()).sendMessages();
    }

    /**
     * Method for send a greetings message to other peers (only used in protocol for enhancements).
     */
    public void sendGreetings() {
        Message msg = new Message(this.protocolVersion, MessageType.GREETINGS, this.peerID);
        try {
            msg.send(this.ipAddressMC, this.portMC);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

 }

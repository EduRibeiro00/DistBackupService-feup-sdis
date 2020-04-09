package peer.protocols;

import peer.FileRestorer;
import peer.messages.Header;
import peer.messages.Message;
import peer.messages.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;

public class Protocol1 extends Protocol {
    protected int numberOfThreads = 20;
    protected ScheduledThreadPoolExecutor executor;

    public Protocol1(int peerID, String ipAddressMC, int portMC, String ipAddressMDB, int portMDB, String ipAddressMDR, int portMDR) {
        super(peerID, ipAddressMC, portMC, ipAddressMDB, portMDB, ipAddressMDR, portMDR);

        this.setVersion("1.0");
        executor = new ScheduledThreadPoolExecutor(numberOfThreads);
    }


    @Override
    public void initiateBackup(String filePath, String modificationDate, int chunkNo, byte[] fileContent, int replicationDeg) {
        String encodedFileId = null;
        try {
            encodedFileId = this.fileManager.insertHashForFile(filePath, modificationDate);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        this.backupChunk(encodedFileId, chunkNo, fileContent, replicationDeg);
    }


    @Override
    public void deleteIfOutdated(String filepath, String modificationDate) {
        String fileID = null;
        try {
            fileID = Header.encodeFileId(filepath + modificationDate);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        // if hashes are different, then the file has been modified and the chunks
        // previously backed up are now outdated. The system will delete them
        String hash = this.fileManager.getHashForFile(filepath);
        if (hash != null && !fileID.equals(hash)) {
            this.initiateDelete(filepath);
        }
    }


    @Override
    protected void backupChunk(String fileId, int chunkNo, byte[] fileContent, int replicationDeg) {
        this.fileManager.setMaxChunkNo(fileId, chunkNo);
        this.chunkManager.setDesiredReplication(fileId, replicationDeg);

        Message msg;
        msg = new Message(this.protocolVersion, MessageType.PUTCHUNK, this.peerID, fileId, chunkNo, replicationDeg, fileContent);

        sendPutChunkMsgLoop(msg, 0, fileId, chunkNo, replicationDeg);
    }

    private void sendPutChunkMsgLoop(Message msg, int iteration, String fileId, int chunkNo, int replicationDeg) {
        if(this.chunkManager.getPerceivedReplication(fileId, chunkNo) < replicationDeg) {
            try {
                msg.send(this.ipAddressMDB, this.portMDB);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (iteration < 5)
                executor.schedule(() -> sendPutChunkMsgLoop(msg, iteration + 1, fileId, chunkNo, replicationDeg),
                        (long) (1000 * Math.pow(2, iteration)),
                        TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public void handleBackup(Message message) {
        Header header = message.getHeader();

        this.chunkManager.setDesiredReplication(header.getFileId(), header.getReplicationDeg());
        this.fileManager.setMaxChunkNo(header.getFileId(), header.getChunkNo());

        try {
            if (!this.fileManager.storeChunk(header.getFileId(), header.getChunkNo(), message.getBody())) {
                return;
            }

            this.chunkManager.addChunkReplication(header.getFileId(), header.getChunkNo(), this.peerID);

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

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void stored(Message message) {
        Header header = message.getHeader();

        this.chunkManager.addChunkReplication(header.getFileId(), header.getChunkNo(), header.getSenderId());
    }


    @Override
    public void initiateRestore(String filepath) {
        // get the file ID of the chunk
        String fileId = this.fileManager.getHashForFile(filepath);
        if(fileId == null) {
            System.err.println("Unknown filepath given");
            return;
        }

        // get the number of chunks this file was divided in
        int maxNumChunks = this.fileManager.getMaxChunkNo(fileId);
        if (maxNumChunks == -1) {
            System.err.println("No information for this file's chunks");
            return;
        }

        // extract filename from filepath
        String filename = Paths.get(filepath).getFileName().toString();

        // create new file restorer
        this.chunkManager.createFileRestorer(filename, fileId, maxNumChunks);

        // send a GETCHUNK for each chunk of the file
        sendGetchunkMsgLoop(fileId, 0, maxNumChunks);
    }


    protected void sendGetchunkMsgLoop(String fileId, int chunkNo, int maxNumChunks) {
        if (chunkNo <= maxNumChunks) {
            try {
                new Message(this.protocolVersion, MessageType.GETCHUNK, this.peerID, fileId, chunkNo).send(
                        this.ipAddressMC, this.portMC
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

            executor.schedule(() -> sendGetchunkMsgLoop(fileId, chunkNo + 1, maxNumChunks),
                    new Random().nextInt(401),
                    TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public void sendChunk(Message message) {
        Header header = message.getHeader();
        String fileId = header.getFileId();
        int chunkNo = header.getChunkNo();

        // if the peer has that chunk saved
        if (this.fileManager.isChunkStored(fileId, chunkNo)) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(CHUNK_SIZE);
            Future<Integer> future;
            MulticastSocket mCastSkt;
            try {
                future = this.fileManager.getChunk(byteBuffer, fileId, chunkNo);

                mCastSkt = new MulticastSocket(this.portMDR);
                mCastSkt.joinGroup(InetAddress.getByName(this.ipAddressMDR));
                mCastSkt.setTimeToLive(1);
            } catch (IOException e) {
                e.printStackTrace(); // TODO: change this
                return;
            }

            long waitTime = new Random().nextInt(401);
            try {
                mCastSkt.setSoTimeout((int) waitTime);
            } catch (SocketException e) {
                e.printStackTrace(); // TODO: change this
                return;
            }

            byte[] buffer = new byte[64500];
            DatagramPacket packet = new DatagramPacket(buffer, 64500);
            try {
                while(waitTime > 0) {
                    long before = System.currentTimeMillis();
                    mCastSkt.receive(packet);
                    waitTime -= System.currentTimeMillis() - before;

                    Message msg = new Message(packet.getData());
                    if(msg.getHeader().getMessageType() == MessageType.CHUNK &&
                            msg.getHeader().getFileId().equals(fileId) &&
                            msg.getHeader().getChunkNo() == chunkNo) {
                        return;
                    }
                    mCastSkt.setSoTimeout((int) waitTime);
                }
            } catch (IOException ignore) {}

            // send message with chunk
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
        }
    }


    @Override
    public void receiveChunk(Message message) {
        Header header = message.getHeader();
        String fileId = header.getFileId();
        int chunkNo = header.getChunkNo();
        byte[] chunkContent = message.getBody();

        // Saves the chunk
        FileRestorer fileRestorer = this.chunkManager.insertChunkForRestore(fileId, chunkNo, chunkContent);

        // If all the file's chunks were saved
        if (fileRestorer != null) {
            // creates and restores the file
            executor.execute(() -> this.fileManager.restoreFileFromChunks(fileRestorer));
            this.chunkManager.deleteChunksForRestore(fileId);
        }
    }


    @Override
    public void initiateDelete(String filepath) {
        String fileId = this.fileManager.getHashForFile(filepath);
        if(fileId == null) {
            System.err.println("Unknown filepath given");
            return;
        }

        Message msg = new Message(this.protocolVersion, MessageType.DELETE, this.peerID, fileId);

        sendDeleteMsgLoop(msg, 0, filepath, fileId);
    }

    private void sendDeleteMsgLoop(Message msg, int iteration, String filepath, String fileId) {
        try {
            msg.send(this.ipAddressMC, this.portMC);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (iteration < 4) {
            executor.schedule(() -> sendDeleteMsgLoop(msg, iteration + 1, filepath, fileId),
                    this.TIMEOUT >> 1,
                    TimeUnit.MILLISECONDS);
        }
        else {
            for (int i = 0; i <= this.fileManager.getMaxChunkNo(fileId); i++) {
                try {
                    this.fileManager.removeChunk(fileId, i);
                } catch (IOException ignored) {}
                this.chunkManager.deletePerceivedReplication(fileId, i);
            }

            this.chunkManager.deleteDesiredReplication(fileId);
            this.fileManager.deleteMaxChunkNo(fileId);
            this.fileManager.deleteHashForFile(filepath);
        }
    }


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
    }


    @Override
    public void reclaim(int newMaximumStorageCapacity) {
        this.fileManager.setMaximumStorageSpace(newMaximumStorageCapacity);

        if(this.fileManager.getAvailableStorageSpace() >= 0)
            return;

        Set<String> toDelete = this.chunkManager.getDeletionOrder(this.peerID);

        // fileId + _ + chunkNo
        for(String fileAndChunk : toDelete) {

            if(this.fileManager.getAvailableStorageSpace() >= 0)
                break;

            String fileId = fileAndChunk.substring(0, fileAndChunk.indexOf('_'));
            int chunkNo = Integer.parseInt(fileAndChunk.substring(fileAndChunk.indexOf('_')+1));

            this.chunkManager.reduceChunkReplication(fileId, chunkNo, this.peerID);

            try {
                this.fileManager.removeChunk(fileId, chunkNo);
                new Message(this.protocolVersion, MessageType.REMOVED, this.peerID, fileId, chunkNo).send(
                        this.ipAddressMC,
                        this.portMC
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    @Override
    public void removed(Message message) {
        String fileId = message.getHeader().getFileId();
        int chunkNo = message.getHeader().getChunkNo();
        int senderId = message.getHeader().getSenderId();

        this.chunkManager.reduceChunkReplication(fileId, chunkNo, senderId);
        int perceivedReplication = this.chunkManager.getPerceivedReplication(fileId, chunkNo);
        int desiredReplication = this.chunkManager.getDesiredReplication(fileId);

        if(!this.fileManager.isChunkStored(fileId, chunkNo) || desiredReplication <= perceivedReplication) {
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(CHUNK_SIZE);
        Future<Integer> future;
        MulticastSocket mCastSkt;
        try {
            future = this.fileManager.getChunk(byteBuffer, fileId, chunkNo);

            mCastSkt = new MulticastSocket(this.portMDB);
            mCastSkt.joinGroup(InetAddress.getByName(this.ipAddressMDB));
            mCastSkt.setTimeToLive(1);
        } catch (IOException e) {
            e.printStackTrace(); // TODO: change this
            return;
        }

        long waitTime = new Random().nextInt(401);
        try {
            mCastSkt.setSoTimeout((int) waitTime);
        } catch (SocketException e) {
            e.printStackTrace(); // TODO: change this
            return;
        }

        byte[] buffer = new byte[64500];
        DatagramPacket packet = new DatagramPacket(buffer, 64500);
        try {
            while(waitTime > 0) {
                long before = System.currentTimeMillis();
                mCastSkt.receive(packet);
                waitTime -= System.currentTimeMillis() - before;

                Message msg = new Message(packet.getData());
                if(msg.getHeader().getMessageType() == MessageType.PUTCHUNK &&
                        msg.getHeader().getFileId().equals(fileId) &&
                        msg.getHeader().getChunkNo() == chunkNo) {
                    return;
                }
                mCastSkt.setSoTimeout((int) waitTime);
            }
        } catch (IOException e) {
            e.printStackTrace(); // TODO: change this
        }

        try {
            future.get();
            byteBuffer.flip();
            byte[] chunkContent = new byte[byteBuffer.limit()];
            byteBuffer.get(chunkContent);
            this.backupChunk(fileId, chunkNo, chunkContent, desiredReplication);
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String state() {
        StringBuilder stateInformation = new StringBuilder();
        stateInformation.append("Files backed up by other peers:\n");
        for(Map.Entry<String, String> entry : this.fileManager.getHashBackedUpFiles()) {
            stateInformation.append("\t" + "Path name: " + entry.getKey() + "\n"); // file path
            stateInformation.append("\t" + "File ID: " + entry.getValue() + "\n"); // backup service ID of the file
            stateInformation.append("\t" + "Desired replication degree: " + this.chunkManager.getDesiredReplication(entry.getValue()) + "\n"); // desired replication degree
            stateInformation.append("\t" + "Chunks of the file: " + "\n");
            int maxChunk = this.fileManager.getMaxChunkNo(entry.getValue());
            // for each chunk
            for (int i = 0; i <= maxChunk; i++) {
                stateInformation.append("\t\t" + "Chunk ID: " + i + "\n"); // chunk ID
                stateInformation.append("\t\t" + "Perceived replication degree: " + this.chunkManager.getPerceivedReplication(entry.getValue(), i) + "\n"); // perceived replication degree
            }
        }

        stateInformation.append("\n");
        stateInformation.append("Files/chunks stored:\n");

        for(Map.Entry<String, ConcurrentSkipListSet<Integer>> entry : this.fileManager.getFileToChunksEntries()) {
            stateInformation.append("\t" + "File ID: " + entry.getKey() + "\n"); // file ID
            for(int chunkNo : entry.getValue()) {
                stateInformation.append("\t\t" + "Chunk ID: " + chunkNo + "\n"); // chunk ID
                stateInformation.append("\t\t" + "Chunk size: " + this.fileManager.getChunkSize(entry.getKey(), chunkNo) + " KB\n"); // chunk size
                stateInformation.append("\t\t" + "Perceived replication degree: " + this.chunkManager.getPerceivedReplication(entry.getKey(), chunkNo) + "\n"); // perceived replication degree
            }
        }

        stateInformation.append("\n");
        stateInformation.append("Maximum storage capacity: " + this.fileManager.getMaximumStorageSpace() + " KB\n");
        stateInformation.append("Available storage capacity: " + this.fileManager.getAvailableStorageSpace() + " KB\n");

        return stateInformation.toString();
    }
}

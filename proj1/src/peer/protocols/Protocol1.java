package peer.protocols;

import peer.messages.Header;
import peer.messages.Message;
import peer.messages.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Protocol1 extends Protocol {

    public Protocol1(int peerID, String ipAddressMC, int portMC, String ipAddressMDB, int portMDB, String ipAddressMDR, int portMDR) throws IOException {
        super(peerID, "1.0", ipAddressMC, portMC, ipAddressMDB, portMDB, ipAddressMDR, portMDR);
    }


    @Override
    public int initiateBackup(String filePath, String modificationDate, int chunkNo, String fileContent, int replicationDeg) {
        String encodedFileId = null;
        try {
            encodedFileId = this.fileManager.insertHashForFile(filePath, modificationDate);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        this.backupChunk(encodedFileId, chunkNo, fileContent, replicationDeg);

        return this.chunkManager.getPerceivedReplication(encodedFileId, chunkNo);
    }


    @Override
    protected void backupChunk(String fileId, int chunkNo, String fileContent, int replicationDeg) {
        this.fileManager.setMaxChunkNo(fileId, chunkNo);
        this.chunkManager.setDesiredReplication(fileId, replicationDeg);
        System.out.println(fileId);
        System.out.println(1);
        System.out.println(this.chunkManager.getDesiredReplication(fileId));

        Message msg;
        msg = new Message(this.protocolVersion, MessageType.PUTCHUNK, this.peerID, fileId, chunkNo, replicationDeg, fileContent);

        for (int i = 0;
             i < 5 && this.chunkManager.getPerceivedReplication(fileId, chunkNo) < replicationDeg;
             i++) {
            try {
                msg.send(this.ipAddressMDB, this.portMDB);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep((long) (this.TIMEOUT * Math.pow(2, i)));
            } catch (InterruptedException ignored) { }

            System.out.println("Ending cycle, have replication degree of " + this.chunkManager.getPerceivedReplication(
                    fileId, chunkNo));
        }
    }


    @Override
    public void initiateDelete(String filePath) {
        String encodedFileId;
        encodedFileId = this.fileManager.getHashForFile(filePath);
        if (encodedFileId == null) {
            return;
        }

        Message msg = new Message(this.protocolVersion, MessageType.DELETE, this.peerID, encodedFileId);

        for (int i = 0; i < 5; i++) {
            try {
                msg.send(this.ipAddressMDB, this.portMDB);
                Thread.sleep(this.TIMEOUT >> 1);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i <= this.fileManager.getMaxChunkNo(encodedFileId); i++) {
            this.chunkManager.deletePerceivedReplication(encodedFileId, i);
        }

        this.chunkManager.deleteDesiredReplication(encodedFileId);
        this.fileManager.deleteMaxChunkNo(encodedFileId);
        this.fileManager.deleteHashForFile(filePath);
    }


    @Override
    public void handleBackup(Message message) {
        Header header = message.getHeader();

        this.chunkManager.setDesiredReplication(header.getFileId(), header.getReplicationDeg());

        try {
            if (!this.fileManager.storeChunk(header.getFileId(), header.getChunkNo(), message.getBody())) {
                return;
            }

            this.chunkManager.addChunkReplication(header.getFileId(), header.getChunkNo(), this.peerID);

            Thread.sleep(new Random().nextInt(401));

            new Message(this.protocolVersion,
                    MessageType.STORED,
                    this.peerID,
                    header.getFileId(),
                    header.getChunkNo()
            ).send(this.ipAddressMC, this.portMC);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

    }


    @Override
    public void stored(Message message) {
        Header header = message.getHeader();

        this.chunkManager.addChunkReplication(header.getFileId(), header.getChunkNo(), header.getSenderId());
    }


    @Override
    public void sendChunk(Message message) {
    }


    @Override
    public void receiveChunk(Message message) {

    }


    @Override
    public void delete(Message message) {
        String fileId = message.getHeader().getFileId();

        ConcurrentSkipListSet<Integer> chunks = this.fileManager.getFileChunks(fileId);

        for (int i = 0; i <= this.fileManager.getMaxChunkNo(fileId); i++) {
            this.chunkManager.deletePerceivedReplication(fileId, (int) chunks.toArray()[i]);

            try {
                this.fileManager.removeChunk(fileId, (int) chunks.toArray()[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        // TODO: ver melhor isto
        System.out.println(this.fileManager.getFileChunks(fileId) == null ? "Successfully deleted all chunks" : "Failed to delete all chunks");

        this.chunkManager.deleteDesiredReplication(fileId);
        this.fileManager.removeFile(fileId);
    }


    @Override
    public void reclaim(int size) {
        this.fileManager.setMaximumStorageSpace(size);

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
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(this.chunkManager.getPerceivedReplication(fileId, chunkNo) == 0) {
                this.initiateDelete(fileId);
                this.chunkManager.deleteDesiredReplication(fileId);
                this.fileManager.removeFile(fileId);
            } else {
                Message msg = new Message(this.protocolVersion, MessageType.REMOVED, this.peerID, fileId, chunkNo);

                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep( new Random().nextInt(401));
                        msg.send(this.ipAddressMC, this.portMC);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
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

        if(this.fileManager.isChunkStored(fileId, chunkNo) && desiredReplication > perceivedReplication) {
            String chunkContent = this.fileManager.getChunk(fileId, chunkNo);

            MulticastSocket mCastSkt;
            try {
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
                }
            } catch (IOException e) {
                e.printStackTrace(); // TODO: change this
            }

            this.backupChunk(fileId, chunkNo, chunkContent, desiredReplication);
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

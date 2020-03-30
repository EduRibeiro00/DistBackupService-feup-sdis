package peer.protocols;

import peer.messages.Header;
import peer.messages.Message;
import peer.messages.MessageType;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

public class Protocol1 extends Protocol {

    public Protocol1(int peerID, String ipAddressMC, int portMC, String ipAddressMDB, int portMDB, String ipAddressMDR, int portMDR) throws IOException {
        super(peerID, "1.0", ipAddressMC, portMC, ipAddressMDB, portMDB, ipAddressMDR, portMDR);
    }

    @Override
    public int initiateBackup(String filePath, int chunkNo, String fileContent, int replicationDeg) {
        String encodedFileId;
        try {
            encodedFileId = Header.encodeFileId(filePath + this.peerID);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return 0;
        }

        this.fileManager.setMaxChunkNo(encodedFileId, chunkNo);

        Message msg;
        msg = new Message(this.protocolVersion, MessageType.PUTCHUNK, this.peerID, encodedFileId, chunkNo, replicationDeg, fileContent);

        for (int i = 0;
                 i < 5 && this.chunkManager.getPerceivedReplication(encodedFileId, chunkNo) < replicationDeg;
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
                    encodedFileId, chunkNo));
        }

        return this.chunkManager.getPerceivedReplication(encodedFileId, chunkNo);
    }

    @Override
    public void initiateDelete(String filePath){
        String encodedFileId;
        try {
            encodedFileId = Header.encodeFileId(filePath + this.peerID);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
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

        List<Integer> chunks = this.fileManager.getFileChunks(fileId);

        for (int i = 0; i <= this.fileManager.getMaxChunkNo(fileId); i++) {
            this.chunkManager.deletePerceivedReplication(fileId, chunks.get(i));

            try {
                this.fileManager.removeChunk(fileId, chunks.get(i));
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Message response = new Message(); // enhancement
        }
        System.out.println(this.fileManager.getFileChunks(fileId) == null ? "Successfully deleted all chunks":"Failed to delete all chunks");

        this.chunkManager.deleteDesiredReplication(fileId);
        this.fileManager.removeFile(fileId);
    }

    @Override
    public void removed(Message message) {

    }
}

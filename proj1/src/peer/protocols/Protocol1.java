package peer.protocols;

import peer.messages.Header;
import peer.messages.Message;
import peer.messages.MessageType;

import java.io.IOException;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Protocol1 extends Protocol {

    public Protocol1(MulticastSocket mCastControl, MulticastSocket mCastBackup, MulticastSocket mCastRestore, int peerID) {
        super(mCastControl, mCastBackup, mCastRestore, peerID, "1.0");
    }

    @Override
    public int initiateBackup(String fileId, int chunkNo, String fileContent, int replicationDeg) {
        int TIMEOUT = 1000;

        Message msg;
        try {
            msg = new Message(this.protocolVersion, MessageType.PUTCHUNK, this.peerID, fileId, chunkNo, replicationDeg, fileContent);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return 0;
        }

        for (int i = 0; i < 5 && this.chunkManager.getReplicationDegree(fileId, chunkNo) < replicationDeg; i++) {
            try {
                msg.send(this.mCastBackup);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep((long) (TIMEOUT * Math.pow(2, i)));
            } catch (InterruptedException ignored) { }
        }

        return this.chunkManager.getReplicationDegree(fileId, chunkNo);
    }


    @Override
    public void handleBackup(Message message) {
        System.out.println("Received BACKUP call");

        Header header = message.getHeader();

        if(header.getSenderId() == this.peerID) {
//            return;
            System.out.println("Received from myself");
        }

        try {
            this.fileManager.storeChunk(header.getFileId(), header.getChunkNo(), message.getBody());
            this.chunkManager.addChunkReplication(header.getFileId(), header.getChunkNo(), this.peerID);

            Thread.sleep(new Random().nextInt(401));

            new Message(this.protocolVersion,
                    MessageType.STORED,
                    this.peerID,
                    header.getFileId(),
                    header.getChunkNo()
            ).send(this.mCastControl);
        } catch (Exception ignored) { }
    }

    @Override
    public void stored(Message message) {
        System.out.println("Received STORED message");

        Header header = message.getHeader();

        this.chunkManager.addChunkReplication(header.getFileId(), header.getChunkNo(), header.getSenderId());
    }

    @Override
    public void sendChunk(Message message) {
        System.out.println("Received GETCHUNK message");

    }

    @Override
    public void receiveChunk(Message message) {

    }

    @Override
    public void delete(Message message) {

    }

    @Override
    public void removed(Message message) {

    }
}

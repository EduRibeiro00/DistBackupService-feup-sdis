package peer.protocols;

import peer.messages.Message;
import peer.messages.MessageType;

import java.io.IOException;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;

public class Protocol1 extends Protocol {

    public Protocol1(MulticastSocket mCastControl, MulticastSocket mCastBackup, MulticastSocket mCastRestore, int peerID) {
        super(mCastControl, mCastBackup, mCastRestore, peerID, "1.0");
    }

    @Override
    public int initiateBackup(String fileId, int chunkNo, String fileContent, int replicationDeg) {
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
        }

        return this.chunkManager.getReplicationDegree(fileId, chunkNo);
    }


    @Override
    public void handleBackup(Message message) {
        System.out.println("Received backup call");
    }

    @Override
    public void stored(Message message) {

    }

    @Override
    public void sendChunk(Message message) {

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

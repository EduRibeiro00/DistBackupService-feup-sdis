package peer.messages;

import peer.messages.Message;
import peer.protocols.Protocol;

import java.net.DatagramPacket;
import java.util.Arrays;


public class MessageHandler {
    private Protocol protocol;

    public MessageHandler(Protocol protocol) {
        this.protocol = protocol;
    }

    public void process(DatagramPacket packet) {

        byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

        Message message;
        try {
            message = new Message(data);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if(message.getHeader().getSenderId() == this.protocol.getPeerID()) {
            return;
        }

        message.setIpAddress(packet.getAddress());
        message.setPort(packet.getPort());

        System.out.println("Received message: " + message.getHeader());

        // Dispatch message to the protocol's method
        switch (message.getHeader().getMessageType()) {
            case PUTCHUNK:
                this.protocol.handleBackup(message);
                break;
            case STORED:
                this.protocol.stored(message);
                break;
            case GETCHUNK:
                this.protocol.sendChunk(message);
                break;
            case CHUNK:
                this.protocol.receiveChunk(message);
                break;
            case DELETE:
                this.protocol.delete(message);
                break;
            case REMOVED:
                this.protocol.removed(message);
                break;
            default:
                break;
        }
    }
}

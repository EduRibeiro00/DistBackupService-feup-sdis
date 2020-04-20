package peer.messages;

import peer.protocols.Protocol;
import java.net.DatagramPacket;
import java.util.Arrays;

/**
 * Class that represents the handling and processing of received messages.
 */
public class MessageHandler {
    private Protocol protocol; /** instance of the protocol */

    /**
     * Constructor of the message handler.
     * @param protocol instance of the protocol
     */
    public MessageHandler(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Method that processes a given received message, calling the correct protocol method based on the type of the message.
     * @param packet datagram packet containing the message received
     */
    public void process(DatagramPacket packet) {

        byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

        Message message;
        try {
            message = new Message(data);
        } catch (Exception e) {
            System.err.println("Message not recognized: ignoring...");
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
            case DELETED:
                this.protocol.receiveDeleted(message);
                break;
            default:
                break;
        }

        if(this.protocol.getVersion().equals("1.1"))
            this.protocol.receivedHeader(message.getHeader());
    }
}

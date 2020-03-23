package peer.messages;

import peer.messages.Message;
import peer.protocols.Protocol;

public class MessageHandler {
    private Protocol protocol;

    public MessageHandler(Protocol protocol) {
        this.protocol = protocol;
    }

    public void process(byte[] data) {
        Message message;
        try {
            message = new Message(data);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

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

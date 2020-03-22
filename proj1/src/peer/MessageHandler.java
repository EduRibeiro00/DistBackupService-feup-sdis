package peer;

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

        Header header = message.getHeader();

        switch (header.getMessageType()) {
            case PUTCHUNK:
                this.protocol.backup();
                break;
            case STORED:
                this.protocol.stored();
                break;
            case GETCHUNK:
                this.protocol.sendChunk();
                break;
            case CHUNK:
                this.protocol.receiveChunk();
                break;
            case DELETE:
                this.protocol.delete();
            case REMOVED:
                this.protocol.removed();
        }
    }

}

package peer.protocols;

import peer.messages.Message;

public class Protocol1 implements Protocol {
    private final String version = "1.0";


    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public void backup(Message message) {
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

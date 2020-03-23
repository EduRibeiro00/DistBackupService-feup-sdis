package peer;

public class Protocol1 implements Protocol {
    private final String version = "1.0";


    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public void backup(Message message) {

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

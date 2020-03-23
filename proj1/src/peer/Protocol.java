package peer;

public interface Protocol {

    //Version handling
    String getVersion();

    // Backup
    void backup(Message message);
    void stored(Message message);

    // Restore
    void sendChunk(Message message);
    void receiveChunk(Message message);

    // Delete
    void delete(Message message);

    // Reclaim
    void removed(Message message);
}

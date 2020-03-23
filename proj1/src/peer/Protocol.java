package peer;

public interface Protocol {
    // Backup
    void backup();
    void stored();

    // Restore
    void sendChunk();
    void receiveChunk();

    // Delete
    void delete();

    // Reclaim
    void removed();
}

package peer;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkManager {
    private final static String filesInfo = "files_info.json";
    private final static String replicationInfo = "replication_info.json";
    private final String directory; // Directory assigned to the peer

    /**
     * Stores the chunks of each file this peer has in his directory
     * key = fileId
     * value = array with ChunkNo
     */
    private ConcurrentHashMap<String, ArrayList<Integer>> chunksTable;

    /**
     * Stores the desired replication degree for each file
     * key = fileId
     * value = desiredReplicationDegree
     */
    private ConcurrentHashMap<String, Integer> desiredReplicationTable;

    /**
     * Stores the perceived replication of each of the chunks
     * key = fileId + _ + chunkNo
     * value = set with ids of the senders
     */
    private ConcurrentHashMap<String, ArrayList<Integer>> perceivedReplicationTable;


    /**
     * Fills the ChunkManager class with the items that exist in the directory given
     * @param directory path to directory were the peer saves his files
     */
    public ChunkManager(String directory) {
        this.directory = directory;
        this.loadFromDirectory();
    }

    /**
     * Updates the perceivedReplicationTable with the possibly new sender
     * @param fileId of the file that was stored
     * @param chunkNo of the file that was stored
     * @param senderId of STORED message received
     */
    public void addChunkReplication(String fileId, int chunkNo, int senderId) {
        ArrayList<Integer> senders = this.perceivedReplicationTable.get(fileId + "_" + chunkNo);

        if(senders == null) {
            senders = new ArrayList<>(senderId);
            this.perceivedReplicationTable.put(fileId + "_" + chunkNo, senders);

        } else if(!senders.contains(senderId)){
            senders.add(senderId);
            this.perceivedReplicationTable.put(fileId + "_" + chunkNo, senders);
        }
    }

    /**
     * Updates the desiredReplicationTable with the new replicationDegree
     * @param fileId of the file sent by the peer
     * @param desiredRepDegree the new replicationDegree
     */
    public void addDesiredReplication(String fileId, int desiredRepDegree) {
        this.desiredReplicationTable.put(fileId, desiredRepDegree);
    }

    /**
     * Checks if the chunk of the file has already been stored in this peer
     * @param fileId of the file to be stored
     * @param chunkNo number of the chunk of the file
     * @return true if this peer has already stored the chunk
     */
    public boolean isChunkStored(String fileId, int chunkNo) {
        return this.chunksTable.get(fileId).contains(chunkNo);
    }

    /**
     * Updates the chunksTable to contain the new chunk that was stored
     * @param fileId file ID of the file that was stored
     * @param chunkNo chunk number of the file that was store
     * @param myId this peer current ID
     */
    public void storeChunk(String fileId, int chunkNo, int myId) {
        ArrayList<Integer> storedChunks = this.chunksTable.get(fileId);

        if(storedChunks == null) {
            storedChunks = new ArrayList<>();
        }

        storedChunks.add(chunkNo);

        this.chunksTable.put(fileId, storedChunks);
        this.addChunkReplication(fileId, chunkNo, myId);
    }

    public int getReplicationDegree(String fileId, int chunkNo) {
        ArrayList<Integer> senders = this.perceivedReplicationTable.get(fileId + "_" + chunkNo);

        return senders == null ? 0 : senders.size();
    }

    // TODO: save and load from files

    /**
     * Fills the tables with the information present in the directory that was passed to the constructor
     */
    private void loadFromDirectory() {
        this.chunksTable = new ConcurrentHashMap<>();
        this.desiredReplicationTable = new ConcurrentHashMap<>();
        this.perceivedReplicationTable = new ConcurrentHashMap<>();

    }

    /**
     * Writes to files in the directory to save the information present on the tables
     */
    private void saveToDirectory() {

    }
}

package peer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkManager {
    private final static String filesInfo = "files_info.json";
    private final static String replicationInfo = "replication_info.json";
    private final String directory; // Directory assigned to the peer

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
     * @param peerID path to directory were the peer saves his files
     */
    public ChunkManager(int peerID) {
        this.directory = String.valueOf(peerID);
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
     *
     * @param fileId
     * @param chunkNo
     * @return replication degree of chunkNo of fileID
     */
    public int getReplicationDegree(String fileId, int chunkNo) {
        ArrayList<Integer> senders = this.perceivedReplicationTable.get(fileId + "_" + chunkNo);

        return senders == null ? 0 : senders.size();
    }

    public void removeChunkReplication(String fileId, int chunk, int peerID) {
        String key = fileId + "_" + chunk;

        List<Integer> peers = this.perceivedReplicationTable.get(key);

        if(peers != null) {
            peers.removeIf(elem -> elem == peerID);
            if(peers.size() == 0){
                this.perceivedReplicationTable.remove(key);
            }
        }
    }

    // TODO: save and load from files

    /**
     * Fills the tables with the information present in the directory that was passed to the constructor
     */
    private void loadFromDirectory() {
        this.desiredReplicationTable = new ConcurrentHashMap<>();
        this.perceivedReplicationTable = new ConcurrentHashMap<>();

    }

    /**
     * Writes to files in the directory to save the information present on the tables
     */
    private void saveToDirectory() {

    }
}

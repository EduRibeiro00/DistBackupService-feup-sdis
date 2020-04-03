package peer;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class ChunkManager {
    private final static String desiredReplicationInfo = "desired_replication_info.json";
    private final static String perceivedReplicationInfo = "perceived_replication_info.json";
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
    private ConcurrentHashMap<String, ConcurrentSkipListSet<Integer>> perceivedReplicationTable;


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
        String key = fileId + "_" + chunkNo;
        ConcurrentSkipListSet<Integer> senders = this.perceivedReplicationTable.computeIfAbsent(key, value -> new ConcurrentSkipListSet<>());

        if(senders.add(senderId)) {
            saveToDirectory();
        }
    }

    /**
     * Updates the desiredReplicationTable with the new replicationDegree
     * @param fileId of the file sent by the peer
     * @param desiredRepDegree the new replicationDegree
     */
    public void setDesiredReplication(String fileId, int desiredRepDegree) {
        this.desiredReplicationTable.put(fileId, desiredRepDegree);
        saveToDirectory();
    }

    /**
     *
     * @param fileId
     * @param chunkNo
     * @return replication degree of chunkNo of fileID
     */
    public int getPerceivedReplication(String fileId, int chunkNo) {
        return this.perceivedReplicationTable.getOrDefault(fileId + "_" + chunkNo, new ConcurrentSkipListSet<>()).size();
    }

    public void deletePerceivedReplication(String fileId, int chunk) {
        String key = fileId + "_" + chunk;

        if(this.perceivedReplicationTable.remove(key) != null){
            saveToDirectory();
        }
    }

    public int getDesiredReplication(String fileId) {
        return this.desiredReplicationTable.getOrDefault(fileId, -1);
    }

    public void deleteDesiredReplication(String fileId) {
        this.desiredReplicationTable.remove(fileId);
    }

    /**
     * Fills the tables with the information present in the directory that was passed to the constructor
     */
    private void loadFromDirectory() {
        // Loading desired replication table
        try {
            FileInputStream desRepFileIn = new FileInputStream("./chunks/" + directory + "/" + desiredReplicationInfo);
            ObjectInputStream desRepObjIn = new ObjectInputStream(desRepFileIn);
            this.desiredReplicationTable = (ConcurrentHashMap<String, Integer>) desRepObjIn.readObject();
            desRepFileIn.close();
            desRepObjIn.close();
        } catch (Exception e) {
            this.desiredReplicationTable = new ConcurrentHashMap<>();
        }

        // Loading perceived replication table
        try {
            FileInputStream percRepFileIn = new FileInputStream("./chunks/" + directory + "/" + perceivedReplicationInfo);
            ObjectInputStream percRepObjIn = new ObjectInputStream(percRepFileIn);
            this.perceivedReplicationTable = (ConcurrentHashMap<String, ConcurrentSkipListSet<Integer>>)percRepObjIn.readObject();
            percRepFileIn.close();
            percRepObjIn.close();
        } catch (Exception e) {
            this.perceivedReplicationTable = new ConcurrentHashMap<>();
        }
    }

    /**
     * Writes to files in the directory to save the information present on the tables
     */
    synchronized private void saveToDirectory() {

        // Saving desired replication table
        try {
            FileOutputStream desRepFileOut = new FileOutputStream("./chunks/" + directory + "/" + desiredReplicationInfo);
            ObjectOutputStream desRepObjOut = new ObjectOutputStream(desRepFileOut);
            desRepObjOut.writeObject(desiredReplicationTable);
            desRepObjOut.close();
            desRepFileOut.close();
        } catch (Exception ignore) {

        }

        // Saving perceived replication table
        try {
            FileOutputStream percRepFileOut = null;
            percRepFileOut = new FileOutputStream("./chunks/" + directory + "/" + perceivedReplicationInfo);
            ObjectOutputStream percRepObjOut = new ObjectOutputStream(percRepFileOut);
            percRepObjOut.writeObject(perceivedReplicationTable);
            percRepObjOut.close();
            percRepFileOut.close();
        } catch (Exception ignore) {

        }
    }
}

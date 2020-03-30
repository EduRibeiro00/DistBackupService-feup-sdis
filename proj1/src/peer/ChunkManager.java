package peer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


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
            saveToDirectory(); //TODO: divide in two
        } else if(!senders.contains(senderId)){
            senders.add(senderId);
            this.perceivedReplicationTable.put(fileId + "_" + chunkNo, senders);
            saveToDirectory(); //TODO: divide in two
        }
    }

    /**
     * Updates the desiredReplicationTable with the new replicationDegree
     * @param fileId of the file sent by the peer
     * @param desiredRepDegree the new replicationDegree
     */
    public void addDesiredReplication(String fileId, int desiredRepDegree) {
        this.desiredReplicationTable.put(fileId, desiredRepDegree);
        saveToDirectory(); //TODO: divide in two
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
            saveToDirectory(); //TODO: divide in two
        }
    }

    /**
     * Fills the tables with the information present in the directory that was passed to the constructor
     */
    private void loadFromDirectory() {

        // Loading desired replication table
        try {
            FileInputStream desRepFileIn = new FileInputStream("./chunks/" + directory + "/" + desiredReplicationInfo);
            ObjectInputStream desRepObjIn = new ObjectInputStream(desRepFileIn);
            this.desiredReplicationTable = (ConcurrentHashMap<String, Integer>)desRepObjIn.readObject();
            desRepFileIn.close();
            desRepObjIn.close();
        } catch (Exception e) {
            this.desiredReplicationTable = new ConcurrentHashMap<>();
        }

        // Loading perceived replication table
        try {
            FileInputStream percRepFileIn = new FileInputStream("./chunks/" + directory + "/" + perceivedReplicationInfo);
            ObjectInputStream percRepObjIn = new ObjectInputStream(percRepFileIn);
            this.perceivedReplicationTable = (ConcurrentHashMap<String, ArrayList<Integer>>)percRepObjIn.readObject();
            percRepFileIn.close();
            percRepObjIn.close();
        } catch (Exception e) {
            this.perceivedReplicationTable = new ConcurrentHashMap<>();
        }

    }

    /**
     * Writes to files in the directory to save the information present on the tables
     */
    private void saveToDirectory() {

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

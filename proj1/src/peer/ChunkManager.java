package peer;

import peer.messages.Message;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class that contains information about the chunks, and methods that manipulate and/or retrieve that information.
 */
public class ChunkManager {
    private final static String desiredReplicationInfo = "desired_replication_info.data";       /** name of the file containing the desired replication of backed up files */
    private final static String perceivedReplicationInfo = "perceived_replication_info.data";   /** name of the file containing the perceived replication of backed up chunks */
    private final static String fileDeletionInfo = "file_deletion_info.data";                   /** name of the file containing information about the file deletions (for delete enhancement) */
    private final String directory;                                                             /** directory assigned to the peer */

    /**
     * Stores the desired replication degree for each file.
     * key = fileId
     * value = desiredReplicationDegree
     */
    private ConcurrentHashMap<String, Integer> desiredReplicationTable;

    /**
     * Stores the perceived replication of each of the chunks.
     * key = fileId + _ + chunkNo
     * value = set with ids of the senders
     */
    private ConcurrentHashMap<String, ConcurrentSkipListSet<Integer>> perceivedReplicationTable;

    /**
     * Temporarily stores the chunks of a file this peer is trying to restore.
     * key = fileID
     * value = object that contains information/data about the restoring procedure
     */
    private ConcurrentHashMap<String, FileRestorer> fileRestoringTable;

    /**
     * Stores the peers that should delete a file but are inaccessible at the moment.
     * key = peer id
     * value = object that contains information/data about the files/chunks to be deleted
     */
    private ConcurrentHashMap<Integer, FileDeleter> fileDeletionList;


    /**
     * Fills the ChunkManager class with the items that exist in the directory given.
     * @param peerId peer identifier
     */
    public ChunkManager(int peerId) {
        this.directory = System.getProperty("user.dir") + "/peer/chunks/" + peerId + "/";
        this.loadFromDirectory();
    }

    /**
     * Updates the perceivedReplicationTable with the possibly new sender.
     * @param fileId file id of the file that was stored
     * @param chunkNo chunk number of the file that was stored
     * @param senderId sender id of STORED message received
     */
    public void addChunkReplication(String fileId, int chunkNo, int senderId) {
        String key = fileId + "_" + chunkNo;
        ConcurrentSkipListSet<Integer> senders = this.perceivedReplicationTable.computeIfAbsent(key, value -> new ConcurrentSkipListSet<>());

        if(senders.add(senderId)) {
            this.saveToDirectory();
        }
    }

    /**
     * Reduces the perceived replication degree for a file's chunk.
     * @param fileId The ID of the file
     * @param chunkNo The number of the chunk
     * @param senderId The ID of the peer storing it
     */
    public void reduceChunkReplication(String fileId, int chunkNo, int senderId) {
        String key = fileId + "_" + chunkNo;
        ConcurrentSkipListSet<Integer> senders = this.perceivedReplicationTable.computeIfAbsent(key, value -> new ConcurrentSkipListSet<>());

        if(senders.remove(senderId)) {
            this.saveToDirectory();
        }
    }

    /**
     * Updates the desiredReplicationTable with the new replicationDegree
     * @param fileId file id of the file sent by the peer
     * @param desiredRepDegree the new replication degree
     */
    public void setDesiredReplication(String fileId, int desiredRepDegree) {
        this.desiredReplicationTable.put(fileId, desiredRepDegree);
        this.saveToDirectory();
    }

    /**
     * Returns the perceived replication degree of a given file's chunk
     * @param fileId the ID of the file
     * @param chunkNo the number of the chunk
     * @return replication degree of chunkNo of fileID
     */
    public int getPerceivedReplication(String fileId, int chunkNo) {
        return this.perceivedReplicationTable.getOrDefault(fileId + "_" + chunkNo, new ConcurrentSkipListSet<>()).size();
    }

    /**
     * Deletes the perceived replication degree of a given file's chunk
     * @param fileId the ID of the file
     * @param chunk the number of the chunk
     */
    public void deletePerceivedReplication(String fileId, int chunk) {
        String key = fileId + "_" + chunk;

        if(this.perceivedReplicationTable.remove(key) != null){
            this.saveToDirectory();
        }
    }

    /**
     * Returns the desired replication degree of a file
     * @param fileId the ID of the file
     * @return the desired replication degree of the file
     */
    public int getDesiredReplication(String fileId) {
        return this.desiredReplicationTable.getOrDefault(fileId, -1);
    }

    /**
     * Deletes the desired replication degree of a file
     * @param fileId The ID of the file
     */
    public void deleteDesiredReplication(String fileId) {
        this.desiredReplicationTable.remove(fileId);
        this.saveToDirectory();
    }


    /**
     * Function that creates a new file restorer for the restoring of a file
     * @param filename Filename
     * @param fileId ID of the file
     * @param maxNumChunks Maximum number of chunks of the file
     */
    public void createFileRestorer(String filename, String fileId, int maxNumChunks) {
        this.fileRestoringTable.put(fileId, new FileRestorer(filename, fileId, maxNumChunks));
    }

    /**
     * Returns true if the fileId is waiting to be stored
     * @param fileId ID of the file
     */
    public boolean isChunkForRestore(String fileId) {
        return this.fileRestoringTable.containsKey(fileId);
    }

    /**
     * Function for temporarily saving a chunk when the peer is trying to restore a file
     * @param fileId
     * @param chunkNo
     * @param chunkContent
     * @returns File restorer object if all chunks of the file are saved, and the peer is ready to restore the file
     */
    public FileRestorer insertChunkForRestore(String fileId, int chunkNo, byte[] chunkContent) {
        FileRestorer fileRestorer = this.fileRestoringTable.get(fileId);
        if (fileRestorer != null && fileRestorer.insertChunkForRestore(chunkNo, chunkContent)) {
            return fileRestorer;
        }

        return null;
    }

    /**
     * Function to delete chunks stored temporarily when the peers restores a file
     * @param fileId
     */
    public void deleteChunksForRestore(String fileId) {
        this.fileRestoringTable.remove(fileId);
    }


    /**
     * Gets the order that the chunks should be deleted in
     * @param peerId The ID of the peer reclaiming space
     * @return A set of fileId_chunkNo strings
     */
    public Set<String> getDeletionOrder(int peerId) {
        Map<String, Integer> unSortedMap = new HashMap<>();

        this.perceivedReplicationTable.forEach((fileAndChunk, senders) -> {
            if(senders.contains(peerId)) {
                int desired = this.desiredReplicationTable.get(getFilePortion(fileAndChunk));
                unSortedMap.put(fileAndChunk, senders.size() - desired);
            }
        });

        LinkedHashMap<String, Integer> reverseSortedMap = new LinkedHashMap<>();

        // descending order
        unSortedMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));

        return reverseSortedMap.keySet();
    }

    /**
     * Gets the file portion of a fileId_chunkNo string
     * @param fileAndChunk A string containing the file ID and chunk number united by an underscore ('_')
     * @return A string containing the file ID
     */
    private String getFilePortion(String fileAndChunk) {
        StringBuffer buffer = new StringBuffer();

        for(int i = 0; i < fileAndChunk.length(); i++) {
            if(fileAndChunk.charAt(i) == '_')
                break;

            buffer.append(fileAndChunk.charAt(i));
        }

        return buffer.toString();
    }

    /**
     * Returns the file storers for the arguments passed.
     * @param fileId file identifier
     * @param highestChunkNo highest chunk number
     * @param peerId peer identifier
     * @return array with the ids of the file storers
     */
    public ArrayList<Integer> getFileStorers(String fileId, int highestChunkNo, int peerId) {
        ArrayList<Integer> holders = new ArrayList<>();

        for(int i = 0; i <= highestChunkNo; i++) {
            String fileAndChunk = fileId + "_" + i;
            ConcurrentSkipListSet<Integer> senders = this.perceivedReplicationTable.getOrDefault(fileAndChunk, new ConcurrentSkipListSet<>());

            for(Integer sender : senders) {
                if(!holders.contains(sender) && sender != peerId)
                    holders.add(sender);
            }
        }

        return holders;
    }


    /**
     * Adds a message to the file deleter associated with a peer.
     * @param peerId peer identifier
     * @param msg message to be added (and later sent to the peer)
     * @param ipAddress IP address for sending the message
     * @param port port for sending the message
     */
    public void addToFileDeleter(Integer peerId, Message msg, String ipAddress, int port) {
        fileDeletionList.computeIfAbsent(peerId, key -> new FileDeleter(ipAddress, port)).addMessage(msg);
        this.saveToDirectory();
    }

    /**
     * Remove messages from a file deleter, for a specific file.
     * @param peerId peer identifier
     * @param fileId file identifier
     */
    public void removeFromFileDeleter(Integer peerId, String fileId) {
        if (this.getFileDeleter(peerId).removeMessages(fileId) ) {
            this.fileDeletionList.remove(peerId);
        }
        this.saveToDirectory();
    }

    /**
     * Retrieves the file deleter associated with a peer. If there is none, returns an empty file deleter.
     * @param peerId peer identifier
     * @return the file deleter
     */
    public FileDeleter getFileDeleter(Integer peerId) {
        return fileDeletionList.getOrDefault(peerId, new FileDeleter());
    }

    /**
     * Remove all file deleters for a specific file.
     * @param fileId file identifier
     */
    public void removeFileDeletion(String fileId) {
        Set<Map.Entry<Integer, FileDeleter>> entrySet = this.fileDeletionList.entrySet();

        for (Map.Entry<Integer, FileDeleter> entry : entrySet) {
            this.removeFromFileDeleter(entry.getKey(), fileId);
        }
    }


    /**
     * Fills the tables with the information present in the directory that was passed to the constructor.
     */
    private void loadFromDirectory() {
        // Creating an empty file restoring table
        this.fileRestoringTable = new ConcurrentHashMap<>();

        // Loading desired replication table
        try {
            FileInputStream desRepFileIn = new FileInputStream(this.directory + desiredReplicationInfo);
            ObjectInputStream desRepObjIn = new ObjectInputStream(desRepFileIn);
            this.desiredReplicationTable = (ConcurrentHashMap<String, Integer>) desRepObjIn.readObject();
            desRepFileIn.close();
            desRepObjIn.close();
        } catch (Exception e) {
            this.desiredReplicationTable = new ConcurrentHashMap<>();
        }

        // Loading perceived replication table
        try {
            FileInputStream percRepFileIn = new FileInputStream(this.directory + perceivedReplicationInfo);
            ObjectInputStream percRepObjIn = new ObjectInputStream(percRepFileIn);
            this.perceivedReplicationTable = (ConcurrentHashMap<String, ConcurrentSkipListSet<Integer>>)percRepObjIn.readObject();
            percRepFileIn.close();
            percRepObjIn.close();
        } catch (Exception e) {
            this.perceivedReplicationTable = new ConcurrentHashMap<>();
        }

        // Loading file deletion list
        try {
            FileInputStream fileDelFileIn = new FileInputStream(this.directory + fileDeletionInfo);
            ObjectInputStream fileDelObjIn = new ObjectInputStream(fileDelFileIn);
            this.fileDeletionList = (ConcurrentHashMap<Integer, FileDeleter>)fileDelObjIn.readObject();
            fileDelFileIn.close();
            fileDelObjIn.close();
        } catch (Exception e) {
            this.fileDeletionList = new ConcurrentHashMap<>();
        }

    }

    /**
     * Writes to files in the directory to save the information present on the tables.
     */
    synchronized private void saveToDirectory() {

        // Saving desired replication table
        try {
            FileOutputStream desRepFileOut = new FileOutputStream(this.directory + desiredReplicationInfo);
            ObjectOutputStream desRepObjOut = new ObjectOutputStream(desRepFileOut);
            desRepObjOut.writeObject(desiredReplicationTable);
            desRepObjOut.close();
            desRepFileOut.close();
        } catch (Exception ignore) {

        }

        // Saving perceived replication table
        try {
            FileOutputStream percRepFileOut = null;
            percRepFileOut = new FileOutputStream(this.directory + perceivedReplicationInfo);
            ObjectOutputStream percRepObjOut = new ObjectOutputStream(percRepFileOut);
            percRepObjOut.writeObject(this.perceivedReplicationTable);
            percRepObjOut.close();
            percRepFileOut.close();
        } catch (Exception ignore) {

        }

        // Saving file deletion list
        try {
            FileOutputStream fileDelFileOut = null;
            fileDelFileOut = new FileOutputStream(this.directory + fileDeletionInfo);
            ObjectOutputStream fileDelObjOut = new ObjectOutputStream(fileDelFileOut);
            fileDelObjOut.writeObject(this.fileDeletionList);
            fileDelObjOut.close();
            fileDelFileOut.close();
        } catch (Exception ignore) {

        }
    }
}

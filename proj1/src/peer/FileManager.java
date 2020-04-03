package peer;

import peer.messages.Header;

import java.security.NoSuchAlgorithmException;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.*;

/**
 * Class that manages the storage and lookup of local files
 */
public class FileManager {
    private final static String fileToChunksInfo = "file_to_chunks.json";
    private final static String highestChunksInfo = "highest_chunks.json";

    /**
     * Stores the chunks of each file this peer has in his directory
     * key = fileId
     * value = array with ChunkNo
     */
    private ConcurrentHashMap<String, ConcurrentSkipListSet<Integer>> fileToChunks;

    /**
     * Stores the highest chunk number of each file received and sent
     * key = fileId
     * value = highest chunk number perceived
     */
    private ConcurrentHashMap<String, Integer> highestChunks;


    /**
     * Stores the generated FileID hashes for each of the backed up files
     * key = file path
     * value = corresponding FileID
     */
    private ConcurrentHashMap<String, String> hashBackedUpFiles;


    /**
     * Stores the storage space used so far
     */
    private int availableStorageSpace;

    /**
     * The ID of the peer of which files are being managed
     */
    private int peerId;


    /**
     * Constructor
     * @param peerId The ID of the peer of which files are going to be managed
     */
    public FileManager(int peerId) {
        this.peerId = peerId;
        this.availableStorageSpace = 100000; // 100 MB of storage for each peer
        this.createDirectory("chunks");
        this.createDirectory("files");
        this.loadFromDirectory();
    }

    /**
     * Returns the available storage space
     * @return an integer symbolizing the used storage space in bytes
     */
    public int getAvailableStorageSpace() {
		return this.availableStorageSpace;
	}

    /**
     * Creates a storage directory for the current peer
     * @return true if successful, false if otherwise
     */
    public boolean createDirectory(String dirName) {
        String path = this.getDirectoryPath(dirName);

        if(Files.exists(Paths.get(path)))
            return true;

        File file = new File(path);
        return file.mkdir();
    }

    /**
     * Return the path to a given chunk in the storage directory
     * @param fileId The ID of the file
     * @param chunkNo The number of the chunk
     * @param chunkContent The chunk's content
     * @return true if successful, false if otherwise
     */
    public boolean storeChunk(String fileId, int chunkNo, String chunkContent) throws IOException {
        if(this.isChunkStored(fileId, chunkNo)) {
            return true;
        }

        // log storage
        if (this.availableStorageSpace < chunkContent.getBytes().length) {
            return false;
        }

        this.addChunkStored(fileId, chunkNo);

        String chunkPath = getChunkPath(fileId, chunkNo);
        File chunk = new File(chunkPath);
        chunk.createNewFile();
        FileWriter chunkWriter = new FileWriter(chunk);
        chunkWriter.write(chunkContent);
        chunkWriter.close();

        this.availableStorageSpace -= chunkContent.getBytes().length;

        this.setMaxChunkNo(fileId, chunkNo);

        return true;    //TODO: add error checking
    }


    /**
     * Inserts hash for file
     * @param filepath The file path
     * @param modificationDate The modification date of the file
     * @return The generated file ID
     */
    public String insertHashForFile(String filepath, String modificationDate) throws NoSuchAlgorithmException {
        System.out.println(filepath);
        System.out.println(modificationDate);
        String fileID = Header.encodeFileId(filepath + modificationDate);
        this.hashBackedUpFiles.put(filepath, fileID);
        return fileID;
    }

    /**
     * Get an already computed fileID for a filepath
     * @param filepath The file path
     * @return The correspondent file ID for the file path
     */
    public String getHashForFile(String filepath) {
        return this.hashBackedUpFiles.get(filepath);
    }

    public void deleteHashForFile(String filepath) {
        this.hashBackedUpFiles.remove(filepath);
    }


    /**
     * Return the content of a chunk
     * @param fileId The ID of the file
     * @param chunkNo The number of the chunk
     * @return A string with the chunk's content
     */
    public String retrieveChunk(String fileId, int chunkNo) throws IOException {
        String chunkPath = getChunkPath(fileId, chunkNo);
        return Files.readString(Paths.get(chunkPath), StandardCharsets.US_ASCII);
    }

    /**
     * Returns the path to the peer's storage directory
     * @return A string containing the path
     */
    public String getDirectoryPath(String dirName) {
        return System.getProperty("user.dir") + "/peer/" + dirName + "/" + this.peerId;
    }

    /**
     * Return the path to a given chunk in the storage directory
     * @param fileId The ID of the file
     * @param chunkNo The number of the chunk
     * @return A string containing the path
     */
    public String getChunkPath(String fileId, int chunkNo) {
        String storageDirectoryPath = getDirectoryPath("chunks");
        return storageDirectoryPath + "/" + fileId + "_" + Integer.toString(chunkNo);
    }

    /**
     * Sets the maximum chunk number for a file
     * @param fileId The ID of the file
     * @param chunkNo The number of the chunk
     */
    public void setMaxChunkNo(String fileId, int chunkNo) {
        this.highestChunks.compute(fileId, (key, value) -> (value == null || chunkNo > value) ? chunkNo : value);
        saveToDirectory();
    }

    /**
     * Returns the maximum chunk number for a file
     * @param fileId The ID of the file
     * @return The number of the chunk
     */
    public int getMaxChunkNo(String fileId) {
        return this.highestChunks.getOrDefault(fileId, -1);
    }

    /**
     * Deletes the maximum chunk number for a file
     * @param fileId The ID of the file
     */
    public void deleteMaxChunkNo(String fileId) {
        this.highestChunks.remove(fileId);
        saveToDirectory();
    }

    /**
     * Returns a list with the chunks of a file
     * @param fileId The ID of the file
     * @return A list with the chunks of the file
     */
    public ConcurrentSkipListSet<Integer> getFileChunks(String fileId) {
        return this.fileToChunks.get(fileId);
    }

    /**
     * Removes a chunk from a file
     * @param fileId The ID of the file
     * @param chunkNo The number of the chunk
     * @return true if successful, false if otherwise
     * @throws IOException
     */
    public boolean removeChunk(String fileId, int chunkNo) throws IOException {
        if (!this.removeChunkStored(fileId, chunkNo)) {
            return false;
        }

        String chunkPath = getChunkPath(fileId, chunkNo);
        Files.deleteIfExists(Paths.get(chunkPath));

        ConcurrentSkipListSet<Integer> chunks = this.fileToChunks.get(fileId);
        chunks.removeIf(elem -> elem.equals(chunkNo));

        System.out.println(chunks.size());

        if (chunks.size() == 0) {
            this.fileToChunks.remove(fileId);
        }

        this.saveToDirectory();

        return true;
    }

    /**
     * Removes a file from the hash tables
     * @param fileId The ID of the file
     */
    public void removeFile(String fileId) {
        this.fileToChunks.remove(fileId);
        this.highestChunks.remove(fileId);

        saveToDirectory();
    }

    /**
     * Adds information that a chunk of a file was stored
     * @param fileId The ID of the file
     * @param chunkNo The number of the chunk
     */
    private void addChunkStored(String fileId, int chunkNo) {
        ConcurrentSkipListSet<Integer> chunks = this.fileToChunks.computeIfAbsent(fileId, value -> new ConcurrentSkipListSet<>());
        chunks.add(chunkNo);

        saveToDirectory();
    }

    /**
     * Removes information that a chunk of a file was stored
     * @param fileId The ID of the file
     * @param chunkNo The number of the chunk
     * @return
     */
    private boolean removeChunkStored(String fileId, int chunkNo) {
        ConcurrentSkipListSet<Integer> chunks = this.fileToChunks.getOrDefault(fileId, new ConcurrentSkipListSet<>());
        return chunks.removeIf(elem -> elem == chunkNo);
    }

    /**
     * Checks if a file's chunk is stored
     * @param fileId The ID of the file
     * @param chunkNo The number of the chunk
     * @return true if it's stored, false if otherwise
     */
    private boolean isChunkStored(String fileId, int chunkNo) {
        ConcurrentSkipListSet<Integer> chunks = this.fileToChunks.getOrDefault(fileId, new ConcurrentSkipListSet<>());
        return chunks.contains(chunkNo);
    }

    /**
     * Fills the tables with the information present in the directory that was passed to the constructor
     */
    private void loadFromDirectory() {

        // Loading highest chunks table
        try {
            FileInputStream highestChunksFileIn = new FileInputStream(this.getDirectoryPath("chunks") + highestChunksInfo);
            ObjectInputStream highestChunksObjIn = new ObjectInputStream(highestChunksFileIn);
            this.highestChunks = (ConcurrentHashMap<String, Integer>) highestChunksObjIn.readObject();
            highestChunksFileIn.close();
            highestChunksObjIn.close();
        } catch (Exception e) {
            this.highestChunks = new ConcurrentHashMap<>();
        }

        // Loading file to chunks table
        try {
            FileInputStream fileToChunksFileIn = new FileInputStream(this.getDirectoryPath("chunks") + fileToChunksInfo);
            ObjectInputStream fileToChunksObjIn = new ObjectInputStream(fileToChunksFileIn);
            this.fileToChunks = (ConcurrentHashMap<String, ConcurrentSkipListSet<Integer>>)fileToChunksObjIn.readObject();
            fileToChunksFileIn.close();
            fileToChunksObjIn.close();
        } catch (Exception e) {
            this.fileToChunks = new ConcurrentHashMap<>();
        }
    }

    /**
     * Writes to files in the directory to save the information present on the tables
     */
    synchronized private void saveToDirectory() {

        // Saving highest chunks table
        try {
            FileOutputStream highestChunksFileOut = new FileOutputStream(this.getDirectoryPath("chunks") + highestChunksInfo);
            ObjectOutputStream highestChunksObjOut = new ObjectOutputStream(highestChunksFileOut);
            highestChunksObjOut.writeObject(this.highestChunks);
            highestChunksObjOut.close();
            highestChunksFileOut.close();
        } catch (Exception ignore) {

        }

        // Saving file to chunks table
        try {
            FileOutputStream fileToChunkFileOut = null;
            fileToChunkFileOut = new FileOutputStream(this.getDirectoryPath("chunks") + fileToChunksInfo);
            ObjectOutputStream fileToChunkObjOut = new ObjectOutputStream(fileToChunkFileOut);
            fileToChunkObjOut.writeObject(this.fileToChunks);
            fileToChunkObjOut.close();
            fileToChunkFileOut.close();
        } catch (Exception ignore) {

        }
    }

}

package peer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.*;

/**
 * Class that manages the storage and lookup of local files
 */
public class FileManager {

    /**
     * Stores the chunks of each file this peer has in his directory
     * key = fileId
     * value = array with ChunkNo
     */
    private ConcurrentHashMap<String, ArrayList<Integer>> fileToChunks;


    /**
     * Stores the storage space used so far
     */
    private int availableStorageSpace;

    /**
     * The ID of the peer of which files are being managed
     */
    private int peerId;

    /**
     * 
     * @param peerId The ID of the peer of which files are going to be managed
     */
    public FileManager(int peerId) {
        this.peerId = peerId;
        this.availableStorageSpace = 100000; // 100 MB of storage for each peer
        this.createDirectory("chunks");
        this.createDirectory("files");
        this.fileToChunks = new ConcurrentHashMap<>();
    }

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

        String chunkPath = getChunkPath(fileId, chunkNo);
        File chunk = new File(chunkPath);
        chunk.createNewFile();
        FileWriter chunkWriter = new FileWriter(chunk);
        chunkWriter.write(chunkContent);
        chunkWriter.close();

        this.availableStorageSpace -= chunkContent.getBytes().length;

        ArrayList<Integer> chunksForFile = this.fileToChunks.get(fileId);

        if(chunksForFile == null) chunksForFile = new ArrayList<>();
        chunksForFile.add(chunkNo);
        this.fileToChunks.put(fileId, chunksForFile);

        return true;    //TODO: add error checking
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

    private boolean isChunkStored(String fileId, int chunkNo) {
        ArrayList<Integer> chunksForFile = this.fileToChunks.get(fileId);
        return (chunksForFile != null) && chunksForFile.contains(chunkNo);
    }

    public List<Integer> getFileChunks(String fileId) {
        return this.fileToChunks.get(fileId);
    }

    public void removeChunk(String fileId, int chunkNo) throws IOException {
        String chunkPath = getChunkPath(fileId, chunkNo);
        Files.deleteIfExists(Paths.get(chunkPath));
    }

    //TODO: save load from files
}

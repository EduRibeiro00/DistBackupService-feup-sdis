package peer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that encompasses the structures and information needed to restore a file, such as its chunks
 */
public class FileRestorer {
    /**
     * Stores the chunks of the file that is to be restored.
     * key = chunkNo
     * value = byte array containing the contents of the file
     */
    private ConcurrentHashMap<Integer, byte[]> fileChunks;

    private String filename;    /** name of the file that is to be restored */
    private String fileId;      /** identifier of the file that is to be restored */
    private int maxNumChunks;   /** total number of chunks that the file will end up having */

    /**
     * Constructor of the file restorer.
     * @param filename name of the file to be restored
     * @param fileId identifier of the file to be restored
     * @param maxNumChunks total number of chunks of the file
     */
    public FileRestorer(String filename, String fileId, int maxNumChunks) {
        this.fileChunks = new ConcurrentHashMap<>();
        this.filename = filename;
        this.fileId = fileId;
        this.maxNumChunks = maxNumChunks;
    }

    /**
     * Get a chunk of the file, given its chunk number.
     * @param chunkNo chunk number
     * @return the contents of the chunk (null if the chunk does not exist)
     */
    public byte[] getChunkForNumber(int chunkNo) {
        return this.fileChunks.get(chunkNo);
    }

    /**
     * Inserts and stores a chunk for restoring the file.
     * @param chunkNo chunk number of the new chunk
     * @param chunkContent content of the chunk
     * @return true if the file restorer now contains all the chunks of the file; false otherwise
     */
    public boolean insertChunkForRestore(int chunkNo, byte[] chunkContent) {
        this.fileChunks.put(chunkNo, chunkContent);
        return this.fileChunks.size() == this.maxNumChunks + 1;
    }

    /**
     * Retrieves the name of the file to be restored.
     * @return the name of the file
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Sets the name of the file to be restored.
     * @param filename name of the file
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Retrieves the identifier of the file to be restored.
     * @return the file identifier
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * Sets the name of the file identifier of the file to be restored.
     * @param fileId the file identifier
     */
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    /**
     * Retrieves the total number of chunks of the file to be restored.
     * @return the number of chunks
     */
    public int getMaxNumChunks() {
        return maxNumChunks;
    }

    /**
     * Sets the total number of chunks of the file to be restored.
     * @param maxNumChunks the number of chunks
     */
    public void setMaxNumChunks(int maxNumChunks) {
        this.maxNumChunks = maxNumChunks;
    }

}

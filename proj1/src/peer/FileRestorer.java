package peer;

import java.util.concurrent.ConcurrentHashMap;

public class FileRestorer {
    private ConcurrentHashMap<Integer, byte[]> fileChunks;
    private String filename;
    private String fileId;
    private int maxNumChunks;

    public FileRestorer(String filename, String fileId, int maxNumChunks) {
        this.fileChunks = new ConcurrentHashMap<>();
        this.filename = filename;
        this.fileId = fileId;
        this.maxNumChunks = maxNumChunks;
    }

    public byte[] getChunkForNumber(int chunkNo) {
        return this.fileChunks.get(chunkNo);
    }

    public boolean insertChunkForRestore(int chunkNo, byte[] chunkContent) {
        this.fileChunks.put(chunkNo, chunkContent);
        return this.fileChunks.size() == this.maxNumChunks+1;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getMaxNumChunks() {
        return maxNumChunks;
    }

    public void setMaxNumChunks(int maxNumChunks) {
        this.maxNumChunks = maxNumChunks;
    }

}

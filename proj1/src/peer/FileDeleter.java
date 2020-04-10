package peer;

import peer.messages.Message;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileDeleter implements Serializable {
    private String ipAddress;
    private int port;
    private ConcurrentHashMap<String, Message> fileToDeletes;

    public FileDeleter() {
        this.ipAddress = "";
        this.port = -1;
        this.fileToDeletes = new ConcurrentHashMap<>();
    }

    public FileDeleter(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.fileToDeletes = new ConcurrentHashMap<>();
    }

    public void addMessage(Message msg) {
        this.fileToDeletes.put(msg.getHeader().getFileId(), msg);
    }

    public boolean removeMessages(String fileId) {
        this.fileToDeletes.remove(fileId);
        return this.fileToDeletes.isEmpty();
    }

    public void sendMessages() {
        for(Map.Entry<String, Message> entry : fileToDeletes.entrySet()) {
            try {
                entry.getValue().send(this.ipAddress, this.port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fileToDeletes = new ConcurrentHashMap<>();
    }

}

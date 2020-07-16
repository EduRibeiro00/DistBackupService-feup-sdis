package peer.protocols;

import peer.*;
import peer.chord.ChordNode;
import peer.chord.ChordRingInfo;
import peer.chord.ChordUtils;
import peer.jsse.ReceiverThread;
import peer.jsse.SenderThread;
import peer.messages.Header;
import peer.messages.Message;
import peer.messages.MessageType;
import peer.task.TaskManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceArray;


/**
 * Class that represents the protocol.
 */
public class Protocol {
    protected int numberOfThreads = 10;                 /** constant with the number of threads for the thread pool */
    protected ScheduledThreadPoolExecutor executor;     /** thread pool executor */
    protected final int CHUNK_SIZE = 64000;             /** chunk size constant */
    protected ChunkManager chunkManager;                /** chunk manager instance */
    protected FileManager fileManager;                  /** file manager instance */
    protected int peerID;                               /** the peer identifier */
    protected ChordRingInfo chordRingInfo;              /** Chord information about other nodes */
    protected TaskManager taskManager;                  /** task manager responsible for delayed operations */


    public Protocol(String ipAddress, int portMC, int portMDB, int portMDR, int portChord) {
        this.peerID = ChordRingInfo.generateHash(ipAddress + "_" + portMC + "_" + portMDB + "_" + portMDR + "_" + portChord);
        System.out.println("Generated peer ID -> " + peerID);

        this.taskManager = new TaskManager();
        this.chordRingInfo = new ChordRingInfo(ipAddress, portMC, portMDB, portMDR, portChord, peerID, taskManager);
        this.fileManager = new FileManager(this.peerID);
        this.chunkManager = new ChunkManager(this.peerID);
        this.executor = new ScheduledThreadPoolExecutor(numberOfThreads);
    }

    /**
     * Get method for the peer identifier.
     * @return identifier of the peer
     */
    public int getPeerID() {
        return this.peerID;
    }

    public ChordRingInfo getChord() {
        return chordRingInfo;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    /**
     * Method to be called when a backup to a file is about to be started.
     * @param filepath Filepath
     * @param modificationDate Modification date of the file
     * @return The encoded file ID of the file
     */
    public String startFileBackup(String filepath, String modificationDate) {
        String encodedFileId = null;
        try {
            encodedFileId = this.fileManager.insertHashForFile(filepath, modificationDate);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
        this.chunkManager.removeFileDeletion(encodedFileId);

        return encodedFileId;
    }


    /**
     * Method to be called by the initiator peer when a backup operation is to be done.
     * @param encodedFileId encoded ID of the file
     * @param chunkNo chunk number
     * @param fileContent content of the file/chunk to be backed up
     * @param replicationDeg desired replication degree for the chunk
     */
    public void initiateBackup(String encodedFileId, int chunkNo, byte[] fileContent, int replicationDeg) {
        this.fileManager.setMaxChunkNo(encodedFileId, chunkNo);

        int replication = replicationDeg - this.chunkManager.getPerceivedReplication(encodedFileId, chunkNo);

        System.out.println("Replication degree" + replication);


        if(replication <= 0){
            return;
        }

        int nodeId = ChordRingInfo.generateHash(encodedFileId + chunkNo);
        System.out.println("ID for chunk " + encodedFileId + "_" + chunkNo + " -> " + nodeId);

        this.chordRingInfo.startFindSuccessor(nodeId,
                (chordNode) -> {

                    if (chordNode.getId() == this.peerID) {
                        chordNode = this.chordRingInfo.getSuccessor();

                        if (chordNode.getId() == this.peerID) {
                            return;
                        }
                    }

                    this.backupChunk(
                            chordRingInfo.getNodeInfo().getIpAddress(),
                            chordRingInfo.getNodeInfo().getPortMC(),
                            chordNode,
                            encodedFileId,
                            chunkNo,
                            fileContent,
                            replication
                    );
                }
        );
    }

    /**
     * Method that tells other peers to backup a specific chunk (to be called by the initiator peer).
     * @param fileId identifier of the file
     * @param chunkNo chunk number
     * @param fileContent content of the file/chunk to be backed up
     */
    protected void backupChunk(String ipAddress, int port, ChordNode node, String fileId, int chunkNo, byte[] fileContent, int replicationDeg) {
        Message msg = new Message(
                MessageType.PUTCHUNK,
                fileId,
                chunkNo,
                replicationDeg,
                ipAddress,
                port,
                fileContent
        );

        SenderThread.sendMessage(
                node.getIpAddress(),
                node.getPortMDB(),
                msg,
                null
        );

    }

    /**
     * Method that tells other peers to delete the chunks of a file if the content of the chunks is outdated.
     * @param filepath path of the file
     * @param modificationDate modification date of the file
     */
    public void deleteIfOutdated(String filepath, String modificationDate) {
        String fileID;
        try {
            fileID = Header.encodeFileId(filepath + modificationDate);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        // if hashes are different, then the file has been modified and the chunks
        // previously backed up are now outdated. The system will delete them
        String hash = this.fileManager.getHashForFile(filepath);
        if (hash != null && !fileID.equals(hash)) {
            this.initiateDelete(filepath);
        }
    }

    /**
     * Method that backs up a chunk, after a PUTCHUNK or GIVECHUNK message is received.
     * @param message message received from the initiator peer (PUTCHUNK or GIVECHUNK)
     */
    public void handleBackup(Message message) {
        Header header = message.getHeader();
        MessageType msgType = header.getMessageType();
        String fileId = header.getFileId();
        int chunkNo = header.getChunkNo();
        String ipAddress = header.getIpAddress();
        int port = header.getPort();
        byte[] body = message.getBody();
        int replication = header.getReplication();

        if (this.fileManager.amFileOwner(fileId) || this.fileManager.isChunkStored(fileId, chunkNo)) {
            this.redirectBackup(message, replication);
            return;
        }



        this.chunkManager.removeFileDeletion(fileId);
        this.fileManager.addFileOwner(fileId, ipAddress, port);
        this.fileManager.setMaxChunkNo(fileId, chunkNo);

        try {
            if (msgType == MessageType.PUTCHUNK) {
                    if (this.fileManager.storeChunk(fileId, chunkNo, body)) {
                        // if it did store the chunk (or it had it stored already), send the STORED message
                        SenderThread.sendMessage(
                                ipAddress,
                                port,
                                new Message(
                                        MessageType.STORED,
                                        fileId,
                                        chunkNo
                                ),
                                null
                        );
                        this.redirectBackup(message, replication - 1);
                        return;
                    }
            }
            else if (msgType == MessageType.GIVECHUNK) {
                // if it didn't have the file but can successfully store it
                if (this.fileManager.storeChunk(fileId, chunkNo, body)) {
                    SenderThread.sendMessage(
                            ipAddress,
                            port,
                            new Message(
                                    MessageType.STORED,
                                    fileId,
                                    chunkNo
                            ),
                            null
                    );
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("Error storing chunk");
            e.printStackTrace();
        }

        // Couldn't backup the chunk
        this.redirectBackup(message, replication);
    }

    /**
     * Method to be called after a STORED message is received.
     * @param message message received from the peer that backed up the chunk
     */
    public void stored(Message message) {
        Header header = message.getHeader();
        this.chunkManager.addChunkReplication(header.getFileId(), header.getChunkNo(), header.getSenderId());
    }

    /**
     * Method that is called by the initiator peer when a CHUNK message is received.
     * @param message message received (CHUNK)
     */
    public void receiveChunk(Message message) {
        Header header = message.getHeader();
        String fileId = header.getFileId();
        int chunkNo = header.getChunkNo();
        byte[] body = message.getBody();

        if(!this.chunkManager.isChunkForRestore(fileId)) {
            return;
        }

        FileRestorer fileRestorer = this.chunkManager.insertChunkForRestore(
                fileId,
                chunkNo,
                body
        );

        // If all the file's chunks were saved
        if (fileRestorer != null) {
            // creates and restores the file
            executor.execute(() -> this.fileManager.restoreFileFromChunks(fileRestorer));
            this.chunkManager.deleteChunksForRestore(fileId);
        }
    }

    /**
     * Method that sends a chunk back to the initiator peer, when a GETCHUNK message is received.
     * @param message message received from the initiator peer (GETCHUNK)
     */
    public void sendChunk(Message message) {
        Header header = message.getHeader();
        String fileId = header.getFileId();
        int chunkNo = header.getChunkNo();
        String ipAddress = header.getIpAddress();
        int port = header.getPort();

        // if the peer has that chunk saved
        if (this.fileManager.isChunkStored(fileId, chunkNo)) {

            try {
                byte[] chunkContent = this.retrieveChunk(fileId, chunkNo);

                // send message with chunk
                SenderThread.sendMessage(
                        ipAddress,
                        port,
                        new Message(
                                MessageType.CHUNK,
                                fileId,
                                chunkNo,
                                chunkContent
                        ),
                        null
                );
            } catch (IOException e) {
                System.err.println("Error getting the chunk");
                e.printStackTrace();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error retrieving chunk from files");
                e.printStackTrace();
            }


        } else {
            this.redirectRestore(message);
        }
    }

    /**
     * Redirects the message given to the successor in the chord ring
     * @param message the message to be redirected
     */
    private void redirectRestore(Message message){
        String fileId = message.getHeader().getFileId();
        int chunkNo = message.getHeader().getChunkNo();
        ChordNode node = this.chordRingInfo.getSuccessor();
        if(this.canPropagate(node, fileId, chunkNo))
            SenderThread.sendMessage(
                    node.getIpAddress(),
                    node.getPortMDR(),
                    message,
                    null
            );
    }

    /**
     * Method to retrieve a chunk from storage
     * @param fileId id of the file
     * @param chunkNo chunk number of the file to retrieve
     * @return the chunk content
     * @throws IOException when
     */
    private byte[] retrieveChunk(String fileId, int chunkNo) throws IOException, InterruptedException, ExecutionException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(CHUNK_SIZE);
        Future<Integer> future;

        future = this.fileManager.getChunk(byteBuffer, fileId, chunkNo);

        int chunkSize = future.get();
        byteBuffer.flip();

        return Arrays.copyOfRange(byteBuffer.array(), 0, chunkSize);
    }

    /**
     * Method to be called by the initiator peer when a restore operation is to be done.
     * @param filepath path of the file
     */
    public void initiateRestore(String filepath) {
        // get the file ID of the chunk
        String fileId = this.fileManager.getHashForFile(filepath);
        if(fileId == null) {
            System.err.println("Unknown filepath given");
            return;
        }

        // get the number of chunks this file was divided in
        int maxNumChunks = this.fileManager.getMaxChunkNo(fileId);
        if (maxNumChunks == -1) {
            System.err.println("No information for this file's chunks");
            return;
        }

        // extract filename from filepath
        String filename = Paths.get(filepath).getFileName().toString();

        // create new file restorer
        this.chunkManager.createFileRestorer(filename, fileId, maxNumChunks);

        // send a GETCHUNK for each chunk of the file
        for (int chunkNo = 0; chunkNo <= maxNumChunks; chunkNo++) {
            Message message = new Message(
                    MessageType.GETCHUNK,
                    fileId,
                    chunkNo,
                    this.chordRingInfo.getNodeInfo().getIpAddress(),
                    this.chordRingInfo.getNodeInfo().getPortMDR()
            );

            sendRestore(message, 0);
        }
    }

    /**
     * Sends the restore message to the peers that are known to store the chunk
     * @param message the message to be sent
     * @param storedIndex the index of the node that stored the chunk in the replication table
     */
    private void sendRestore(Message message, int storedIndex){
        int id = this.chunkManager.getFileStorer(message.getHeader().getFileId(), message.getHeader().getChunkNo(), storedIndex);
        if(id == -1) {
            return;
        }

        int nextIndex = storedIndex + 1;

        this.chordRingInfo.startFindSuccessor(
            id,
            (chordNode) -> SenderThread.sendMessage(
                    chordNode.getIpAddress(),
                    chordNode.getPortMC(),
                    message,
                    () -> sendRestore(message, nextIndex)
            ));
    }

    /**
     * Method to be called by the initiator peer when a delete operation is to be done.
     * @param filepath path of the file
     */
    public void initiateDelete(String filepath) {
        String fileId = this.fileManager.getHashForFile(filepath);

        if(fileId == null) {
            System.err.println("Unknown filepath given");
            return;
        }

        Message msg = new Message(
                MessageType.DELETE,
                fileId,
                chordRingInfo.getNodeInfo().getIpAddress(),
                chordRingInfo.getNodeInfo().getPortMC()
        );

        ArrayList<Integer> storers = this.chunkManager.getFileStorers(fileId, this.fileManager.getMaxChunkNo(fileId), this.peerID);

        for(Integer storer : storers){
            int localStorer = storer;
            this.chordRingInfo.startFindSuccessor(
                    localStorer,
                    (chordNode) -> {
                        if(chordNode.getId() == localStorer)
                            SenderThread.sendMessage(
                                    chordNode.getIpAddress(),
                                    chordNode.getPortMC(),
                                    msg,
                                    () -> this.chunkManager.addToFileDeleter(localStorer, msg)
                            );
                    }
            );
        }

        for (int i = 0; i <= this.fileManager.getMaxChunkNo(fileId); i++) {
            this.chunkManager.deletePerceivedReplication(fileId, i);
        }

        this.fileManager.deleteMaxChunkNo(fileId);
        this.fileManager.deleteFileForHash(fileId);
    }


    /**
     * Method to be called when a DELETE message is received.
     * @param message message received (DELETE)
     */
    public void delete(Message message) {
        Header header = message.getHeader();
        String fileId = header.getFileId();
        boolean deleted = false;

        if(this.fileManager.getMaxChunkNo(fileId) == -1) {
            SenderThread.sendMessage(
                    header.getIpAddress(),
                    header.getPort(),
                    new Message(
                            MessageType.DELETED,
                            fileId),
                    null
            );
            return;
        }

        for (int i = 0; i <= this.fileManager.getMaxChunkNo(fileId); i++) {
            try {
                if(this.fileManager.removeChunk(fileId, i)) {
                    deleted = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.fileManager.removeFile(fileId);

        // if it had at least one chunk and it deleted it, send DELETED message
        if (deleted) {
            SenderThread.sendMessage(
                    header.getIpAddress(),
                    header.getPort(),
                    new Message(
                            MessageType.DELETED,
                            fileId),
                    null
            );
        }
    }


    /**
     * Method to be called when a DELETED message is received.
     * @param message message received (DELETED)
     */
    public void receiveDeleted(Message message) {
        Header header = message.getHeader();
        this.chunkManager.removeFromFileDeleter(header.getSenderId(), header.getFileId());
    }


    /**
     * Method to be called by the initiator peer when a reclaim operation is to be done.
     * @param newMaximumStorageCapacity new maximum storage capacity for the peer
     */
    public void reclaim(int newMaximumStorageCapacity) {
        this.fileManager.setMaximumStorageSpace(newMaximumStorageCapacity);

        if(this.fileManager.getAvailableStorageSpace() >= 0)
            return;

        Set<String> toDelete = this.chunkManager.getDeletionOrder(this.fileManager.getChunkSizes());

        // fileId + _ + chunkNo
        for(String fileAndChunk : toDelete) {

            if(this.fileManager.getAvailableStorageSpace() >= 0)
                break;

            String fileId = fileAndChunk.substring(0, fileAndChunk.indexOf('_'));
            int chunkNo = Integer.parseInt(fileAndChunk.substring(fileAndChunk.indexOf('_')+1));

            try {
                ChordNode successor = this.chordRingInfo.getSuccessor();
                AddressRecord parentRecord = this.fileManager.getFileOwner(fileId);
                if (parentRecord == null)
                    continue;

                byte[] chunkContent = this.retrieveChunk(fileId, chunkNo);

                SenderThread.sendMessage(
                        successor.getIpAddress(),
                        successor.getPortMDB(),
                        new Message(MessageType.GIVECHUNK, fileId, chunkNo, parentRecord.getIpAddress(), parentRecord.getPort(), this.peerID, chunkContent),
                        null
                );
                this.removeChunk(fileId, chunkNo);
            } catch (IOException | InterruptedException | ExecutionException e) {
                System.err.println("Failed to remove chunk ");
                e.printStackTrace();
            }

        }
    }

    /**
     * Method to remove a chunk and send REMOVED message to owner
     * @param fileId ID of the file to remove
     * @param chunk chunk number of the file to be removed
     * @throws IOException when an error occurs during file deletion
     */
    private void removeChunk(String fileId, int chunk) throws IOException {
        AddressRecord addRecord = this.fileManager.getFileOwner(fileId);
        this.fileManager.removeChunk(fileId, chunk);

        if(addRecord == null) {
            System.err.println("Unknown owner of " + fileId);
        } else {
            SenderThread.sendMessage(
                    addRecord.getIpAddress(),
                    addRecord.getPort(),
                    new Message(MessageType.REMOVED, fileId, chunk),
                    null
            );
        }

    }

    /**
     * Method to be called when a REMOVED message is received.
     * @param message message received (REMOVED)
     */
    public void removed(Message message) {
        String fileId = message.getHeader().getFileId();
        int chunkNo = message.getHeader().getChunkNo();
        int senderId = message.getHeader().getSenderId();

        if(!this.fileManager.amFileOwner(fileId)){
            return;
        }

        this.chunkManager.reduceChunkReplication(fileId, chunkNo, senderId);
    }

    /**
     * Method to be called when receiving a PUTCHUNK or GIVECHUNK message to propagate the backup with given replication
     * @param message the PUTCHUNK/GIVECHUNK message received
     * @param replication the new replication of the message
     */
    private void redirectBackup(Message message, int replication) {
        MessageType msgType = message.getHeader().getMessageType();
        ChordNode node = this.chordRingInfo.getSuccessor();

        boolean canPropagate = false;
        if (msgType == MessageType.PUTCHUNK) {
            if (replication <= 0)
                return;

            String fileId = message.getHeader().getFileId();
            int chunkNo = message.getHeader().getChunkNo();
            canPropagate = this.canPropagate(node, fileId, chunkNo);
        }
        else if (msgType == MessageType.GIVECHUNK) {
            int barrierId = message.getHeader().getBarrierId();
            canPropagate = this.canPropagate(node, barrierId);
        }

        if(canPropagate) {

            if (msgType == MessageType.PUTCHUNK)
                message.getHeader().setReplication(replication);

            SenderThread.sendMessage(
                    node.getIpAddress(),
                    node.getPortMDB(),
                    message,
                    null
            );
        }
    }

    /**
     * Checks to see if the message can still be propagated
     * @param node the successor of the peer
     * @param fileId id of the message's file
     * @param chunkNo number of the chunk related to the message's file
     * @return Returns true when the message can be propagated. False when not.
     */
    private boolean canPropagate(ChordNode node, String fileId, int chunkNo) {
        int key = ChordRingInfo.generateHash(fileId + chunkNo);
        return node != null && node.getId() != this.peerID && !ChordUtils.isBetweenInc(this.peerID, node.getId(), key);
    }

    /**
     *
     * @param node
     * @param barrierId
     * @return
     */
    private boolean canPropagate(ChordNode node, int barrierId) {
        return node.getId() != barrierId;
    }


    /**
     * Method for receiving and parsing a header. Sends to a peer messages to delete chunks it was supposed to delete.
     * @param header received header
     */
    public void receivedHeader(Header header) {
        Set<Map.Entry<String, Message>> filesToDelete = this.chunkManager.getFileDeleter(header.getSenderId()).getFileToDeletes();

        if(!filesToDelete.isEmpty()){
            this.chordRingInfo.startFindSuccessor(
                    header.getSenderId(),
                    (chordNode) -> {
                        if (header.getSenderId() == chordNode.getId()) {
                            for (Map.Entry<String, Message> entry : filesToDelete)
                                SenderThread.sendMessage(
                                        chordNode.getIpAddress(),
                                        chordNode.getPortMC(),
                                        entry.getValue(),
                                        null
                                );
                        }
                    }
            );
        }
    }


    /**
     * Method to be called when a node leaves the ring voluntarily; it
     * redistributes the keys that the leaving node is successor to its predecessor;
     * sets predecessor's successor as the leaving node's successor,
     * sets node's successor's predecessor, as the node's predecessor.
     */
    public void exit() {
        System.out.println("Ending Chord tasks...");
        this.chordRingInfo.endTasks();

        System.out.println("Passing data to nodes...");
        this.reclaim(0);

        System.out.println("Updating predecessor and successor...");
        this.chordRingInfo.exit();

        System.out.println("Terminating the receiver thread");
        ReceiverThread.exit();

        System.out.println("Terminating sender threads");
        SenderThread.exit();

        System.out.println("\n----------\nPeer terminated successfully!\n----------\n");
        System.exit(0);
    }

    /**
     * Method to be called by the a peer when its current state is requested.
     * @return string containing information about the current state of the peer
     */
    public String state() {
        StringBuilder stateInformation = new StringBuilder();
        stateInformation.append("Files backed up by other peers:\n");
        for(Map.Entry<String, String> entry : this.fileManager.getHashBackedUpFiles()) {
            stateInformation.append("\t" + "Path name: ").append(entry.getKey()).append("\n"); // file path
            stateInformation.append("\t" + "File ID: ").append(entry.getValue()).append("\n"); // backup service ID of the file
            stateInformation.append("\t" + "Chunks of the file: " + "\n");
            int maxChunk = this.fileManager.getMaxChunkNo(entry.getValue());
            // for each chunk
            for (int i = 0; i <= maxChunk; i++) {
                stateInformation.append("\t\t" + "Chunk ID: ").append(i).append("\n"); // chunk ID

                ConcurrentSkipListSet<Integer> storerIdsList = this.chunkManager.getPerceivedReplicationForChunk(entry.getValue(), i);
                String storerIdsString = "-";
                StringBuilder storerIds = new StringBuilder();
                for (Integer storerId : storerIdsList) {
                    storerIds.append(storerId).append(" ");
                }
                if (!storerIds.toString().isBlank())
                    storerIdsString = storerIds.toString();

                stateInformation.append("\t\t" + "Perceived replication degree: ").append(this.chunkManager.getPerceivedReplication(entry.getValue(), i)).append("\n"); // perceived replication degree
                stateInformation.append("\t\t" + "IDs of the storers: ").append(storerIdsString).append("\n"); // IDs of the storers
            }
        }

        stateInformation.append("\n");
        stateInformation.append("Files/chunks stored:\n");

        for(Map.Entry<String, ConcurrentSkipListSet<Integer>> entry : this.fileManager.getFileToChunksEntries()) {
            AddressRecord addRecord = this.fileManager.getFileOwner(entry.getKey());
            stateInformation.append("\t" + "File ID: ").append(entry.getKey()).append("\n"); // file ID
            String addRecordString = "-";
            if (addRecord != null)
                addRecordString = addRecord.getIpAddress() + " " + addRecord.getPort();
            stateInformation.append("\t" + "IP Address and port of the initiator: ").append(addRecordString).append("\n"); // file ID
            for(int chunkNo : entry.getValue()) {
                stateInformation.append("\t\t" + "Chunk ID: ").append(chunkNo).append("\n"); // chunk ID
                stateInformation.append("\t\t" + "Chunk size: ").append(this.fileManager.getChunkSize(entry.getKey(), chunkNo)).append(" KB\n"); // chunk size
            }
        }

        stateInformation.append("\n");
        stateInformation.append("Maximum storage capacity: ").append(this.fileManager.getMaximumStorageSpace()).append(" KB\n");
        stateInformation.append("Available storage capacity: ").append(this.fileManager.getAvailableStorageSpace()).append(" KB\n");

        stateInformation.append("-------\nChord Information:\n");
        stateInformation.append("\tPeer/Node ID: ").append(peerID).append("\n");

        String predecessorID = "-";
        if (chordRingInfo.getPredecessor() != null)
            predecessorID = String.valueOf(chordRingInfo.getPredecessor().getId());

        stateInformation.append("\tPredecessor ID: ").append(predecessorID).append("\n");

        String chordSuccessorsString = "-";
        ChordNode successor = chordRingInfo.getSuccessor();
        if (successor != null)
            chordSuccessorsString = String.valueOf(successor.getId());

        stateInformation.append("\tSuccessor ID: ").append(chordSuccessorsString).append("\n");

        String chordFingersString = "-";
        StringBuilder chordFingers = new StringBuilder();
        AtomicReferenceArray<ChordNode> fingers = chordRingInfo.getFingers();
        for (int i = 0; i < fingers.length(); i++) {
            ChordNode finger = fingers.get(i);
            if (finger != null)
                chordFingers.append(finger.getId()).append(" ");
        }
        if (!chordFingers.toString().isBlank())
            chordFingersString = chordFingers.toString();

        stateInformation.append("\tFinger IDs: ").append(chordFingersString).append("\n");

        return stateInformation.toString();
    }
 }

package peer.chord;
import peer.task.Task;
import peer.task.TaskManager;
import peer.jsse.SenderThread;
import peer.jsse.error.PredecessorError;
import peer.messages.Message;
import peer.messages.MessageType;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Class that represents the distributed hash table used by the Chord protocol.
 */
public class ChordRingInfo {
    /**
     * Sender Id of the peer, for sending messages.
     */
    private final int senderId;

    /**
     * Constant with the number of threads for the thread pool.
     */
    private static final int NUMBER_OF_THREADS = 20;

    /**
     * Constant with the delay (in milliseconds) for the periodically ran methods.
     */
    private static final int DELAY = 15000;

    /**
     * Thread pool executor.
     */
    protected ScheduledThreadPoolExecutor executor;

    /**
     * Task manager that keeps a record of the pending Chord requests.
     */
    private final TaskManager taskManager;

    /**
     * Number of bits for each Chord ID. The system allows a maximum of 2^m nodes.
     */
    private static final int m = 6;

    /**
     * Static integer that denotes the next entry of the finger table that needs to be checked.
     */
    private static int next = 0;

    /**
     * Node information relative to the current peer.
     */
    private final ChordNode nodeInfo;

    /**
     * Predecessor of the node.
     */
     private ChordNode predecessor;

    /**
     * Routing table (fingers) of the node; nodes that this node can access directly.
     */
     private final AtomicReferenceArray<ChordNode> fingers;

     private ScheduledFuture<?> scheduledStabilize;

     private ScheduledFuture<?> scheduledFixFingers;

     private ScheduledFuture<?> scheduledStartCheckPredecessor;

    /**
     * Construction of the Chord ring information.
     * @param ipAddress IP address of the peer's Chord node
     * @param portMC MC port of the peer's node
     * @param portMDB MDB port of the peer's node
     * @param portMDR MDR port of the peer's node
     * @param portChord Chord port of the peer's node
     * @param senderId Sender ID, for sending messages
     * @param taskManager Task manager
     */
    public ChordRingInfo(String ipAddress, int portMC, int portMDB, int portMDR, int portChord, int senderId, TaskManager taskManager) {
        this.senderId = senderId;
        this.nodeInfo = new ChordNode(ipAddress, portMC, portMDB, portMDR, portChord, senderId);
        this.predecessor = null;
        this.fingers = new AtomicReferenceArray<>(m);
        this.taskManager = taskManager;
        this.executor = new ScheduledThreadPoolExecutor(NUMBER_OF_THREADS);

        // start background tasks that run periodically
        this.scheduledStabilize = this.executor.scheduleAtFixedRate(this::stabilize, DELAY, DELAY, TimeUnit.MILLISECONDS);
        this.scheduledFixFingers = this.executor.scheduleAtFixedRate(this::fixFingers, DELAY + 2000, DELAY, TimeUnit.MILLISECONDS);
        this.scheduledStartCheckPredecessor = this.executor.scheduleAtFixedRate(this::startCheckPredecessor, DELAY + 4000, DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * Getter for the M variable (number of bits for Chord).
     * @return Number M
     */
    public static int getM() {
        return m;
    }

    /**
     * Generates SHA-1 hash, making an identifier for the Chord ring.
     * @param key String key
     * @return Identifier for the Chord ring
     */
    public static int generateHash(String key) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(key.getBytes(StandardCharsets.UTF_8));
            String sha1 = String.format("%040x", new BigInteger(1, digest.digest()));
            int hashCode = sha1.hashCode();
            if (hashCode < 0) {
                hashCode = -hashCode;
            }
            return (hashCode % (int) Math.pow(2, m));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getFileOffset(int replicationDegree) {
        int maxNumNodes = (int) Math.pow(2, m);
        return maxNumNodes / replicationDegree;
    }

    public void setPredecessor(ChordNode predecessor) {
        this.predecessor = predecessor;
    }

    public void setSuccessor(ChordNode successor) {
        fingers.set(0, successor);
    }

    // ----------------------------------------------------------------------------------------------------
    // ----------------------
    // Creating and joining
    // ----------------------

    /**
     * Method that makes the node create a new Chord ring, or join one.
     * @param initIpAddress IP address of the node we should contact in order to join the Chord ring
     * @param initPort Port number of the node we should contact in order to join the Chord ring
     */
    public void createOrJoin(String initIpAddress, int initPort) {
        // join or create Chord ring
        if (initIpAddress != null && initPort != -1) {
            predecessor = null;
            this.taskManager.addTask(nodeInfo.getId(), this::handleSetSuccessor);
            SenderThread.sendMessage(
                    initIpAddress,
                    initPort,
                    new Message(
                            MessageType.FIND_SUCC,
                            nodeInfo.getId(),
                            nodeInfo.getIpAddress(),
                            nodeInfo.getPortChord()
                    ),
                    null
            );
        }
        else {
            predecessor = null;
            fingers.set(0, nodeInfo); // assign ourselves as our successor
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // ----------------------
    // Successor finding
    // ----------------------

    /**
     * Method that finds the successor node for a given key.
     * @param keyHash Key in the Chord ring
     * @param task The task for this key
     * @return The object that is the successor for that key; null otherwise
     */
    public ChordNode startFindSuccessor(int keyHash, Task task) {
        return findSuccessor(keyHash, nodeInfo.getIpAddress(), nodeInfo.getPortChord(), task);
    }

    /**
     * Method to be called after a FIND_SUCC message is received.
     * @param keyHash Identifier in the Chord ring.
     * @param ipAddress IP Address of the node to contact after its successor is found
     * @param port Port number of the node to contact after its successor is found
     */
    public void handleFindSuccessor(int keyHash, String ipAddress, int port) {
        // try to fund successor; if found, return value to the initiator node
        ChordNode node = findSuccessor(keyHash, ipAddress, port, null);
        if (node != null) {
            SenderThread.sendMessage(
                    ipAddress,
                    port,
                    new Message(
                            MessageType.RTRN_SUCC,
                            node,
                            keyHash
                    ),
                    null
            );
        }
    }

    /**
     * Generic function to get successor of a certain key.
     * @param keyHash Key in the Chord ring
     * @param ipAddress IP Address of the node to contact after its successor is found
     * @param port Port number of the node to contact after its successor is found
     * @param task The task for this key
     * @return The object that is the successor for that key; null otherwise
     */
    public ChordNode findSuccessor(int keyHash, String ipAddress, int port, Task task) {
        // check successor
        ChordNode successor = getSuccessor();
        if(successor != null && ChordUtils.isBetweenInc(nodeInfo.getId(), successor.getId(), keyHash)) {
            if(task != null) task.complete(successor);
            return successor;
        }

        // get closest preceding node
        ChordNode closest = closestPrecedingNode(keyHash);
        if (closest.getId() == nodeInfo.getId()) {
            if(task != null) task.complete(nodeInfo);
            return nodeInfo;
        }

        // add task as callback function from when we receive the response for this key
        if (task != null)
            this.taskManager.addTask(keyHash, task);

        // send message to the closest preceding node in order to find out successor
        SenderThread.sendMessage(
                closest.getIpAddress(),
                closest.getPortChord(),
                new Message(
                        MessageType.FIND_SUCC,
                        keyHash,
                        ipAddress,
                        port
                ),
                null
        );

        // no "local" value; returning null
        return null;
    }

    /**
     * Method that accesses the successor list and finger table in order to find out the
     * closest preceding node for a certain key.
     * @param keyHash Identifier in the Chord ring
     * @return The object that is the successor for that key; null otherwise
     */
    public ChordNode closestPrecedingNode(int keyHash) {
        // check fingers
        for (int i = fingers.length() - 1; i >= 0; i--) {
            ChordNode next = fingers.get(i);
            if (next != null && ChordUtils.isBetween(nodeInfo.getId(), keyHash, next.getId())) {
                return next;
            }
        }

        // return own peer
        return nodeInfo;
    }

    /**
     * Method to be called after a RTRN_SUCC message is received. Updates the node's successor.
     * @param successor New successor of the node
     */
    public void handleSetSuccessor(ChordNode successor) {
        ChordNode oldSuccessor = getSuccessor();
        if (oldSuccessor == null || oldSuccessor.getId() == nodeInfo.getId() ||
                ChordUtils.isBetween(nodeInfo.getId(), oldSuccessor.getId(), successor.getId())) {
            fingers.set(0, successor);
        }
        else {
            successor = oldSuccessor;
        }

        // notify the successor that we might be its predecessor
        SenderThread.sendMessage(
                successor.getIpAddress(),
                successor.getPortChord(),
                new Message(
                        MessageType.NOTIFY,
                        nodeInfo
                ),
                null
        );
    }

    // ----------------------------------------------------------------------------------------------------
    // -------------------------
    // Method ran periodically
    // -------------------------

    /**
     * Method to be run periodically, that asks the direct successor for its predecessor,
     * in order to know about new nodes that came in the Chord ring.
     */
    public void stabilize() {
        System.out.println("----\nRunning stabilize\n----\n");
        ChordNode closestSuccessor = getSuccessor();

        if (closestSuccessor == null || closestSuccessor.getId() == nodeInfo.getId())
            return;

        // ask successor for its predecessor (it might be our new successor)
        SenderThread.sendMessage(
                closestSuccessor.getIpAddress(),
                closestSuccessor.getPortChord(),
                new Message(
                    MessageType.GET_PRED,
                    nodeInfo
                ),
                null
        );
    }


    public void handleGetPred(ChordNode node) {
        if(this.predecessor == null) {
            this.predecessor = node;
        }

        SenderThread.sendMessage(
                node.getIpAddress(),
                node.getPortChord(),
                new Message(
                    MessageType.RTRN_PRED,
                    this.predecessor
                ),
                null
        );
    }

    public void handleNotify(ChordNode possiblePredecessor) {
        if (predecessor == null ||
                ChordUtils.isBetween(predecessor.getId(), nodeInfo.getId(), possiblePredecessor.getId())) {
            predecessor = possiblePredecessor;
        }

        // if we have no successors, then the predecessor might also be our successor
        ChordNode oldSuccessor = getSuccessor();
        if (oldSuccessor == null || oldSuccessor.getId() == nodeInfo.getId()) {
            fingers.set(0, possiblePredecessor);
        }
    }

    /**
     * Method that fixes the entries of the finger tables.
     */
    public void fixFingers() {
        int nextCopy = next;

        int wantedFingerKeyHash = this.nodeInfo.getId() + (int) Math.pow(2, nextCopy);
        wantedFingerKeyHash = (wantedFingerKeyHash % (int) Math.pow(2, m));
        System.out.println("----\nRunning fix fingers with index = " + nextCopy + " (hash = " + wantedFingerKeyHash + ")\n----\n");

        startFindSuccessor(wantedFingerKeyHash,
                (chordNode) -> assignFinger(nextCopy, chordNode));

        next = ++next >= m ? 0 : next;
    }

    /**
     * Task for a key lookup that assigns the response (a node) as a finger
     * @param index The index of the finger to assign
     * @param response The Chord Node to be assigned
     */
    public void assignFinger(int index, ChordNode response) {
        System.out.println("----\nAssign finger with index = " + index + " (key of node = " + response.getId() + ")\n----\n");
        fingers.set(index, response);
    }

    /**
     * Checks if the predecessor of the node is still in the ring.
     */
    public void startCheckPredecessor() {
        System.out.println("----\nRunning start check predecessor\n----\n");

        if (predecessor != null)
            SenderThread.sendMessage(
                    predecessor.getIpAddress(),
                    predecessor.getPortChord(),
                    new Message(MessageType.CHECK_ACTIVE),
                    new PredecessorError(this)
            );
    }

    public void handlePredecessorError() {
        ChordNode successor = getSuccessor();
        if (successor != null && successor.getId() == this.predecessor.getId()) {
            fingers.set(0, nodeInfo);
        }
        predecessor = null;
    }

    // ----------------------------------------------------------------------------------------------------
    // ------------------
    // Exiting
    // ------------------

    public void exit() {
        ChordNode successor = fingers.get(0);

        // Update predecessor's successor
        SenderThread.sendMessage(
                predecessor.getIpAddress(),
                predecessor.getPortChord(),
                new Message(MessageType.SET_SUCC, successor),
                null
        );

        // Update successor's predecessor
        SenderThread.sendMessage(
                successor.getIpAddress(),
                successor.getPortChord(),
                new Message(MessageType.SET_PRED, predecessor),
                null
        );
    }

    public void endTasks() {
        this.scheduledStabilize.cancel(false);
        this.scheduledFixFingers.cancel(false);
        this.scheduledStartCheckPredecessor.cancel(false);
    }

    // ----------------------------------------------------------------------------------------------------
    // ------------------
    // Getters
    // ------------------

    public ChordNode getNodeInfo() {
        return nodeInfo;
    }

    public ChordNode getPredecessor() {
        return predecessor;
    }

    public ChordNode getSuccessor() {
        return fingers.get(0);
    }

    public AtomicReferenceArray<ChordNode> getFingers() {
        return fingers;
    }
}

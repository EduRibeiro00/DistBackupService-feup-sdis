package peer.messages;

/**
 * Enum containing the different types of messages that a peer can send/receive.
 */
public enum MessageType {
    PUTCHUNK,
    GETCHUNK,
    GIVECHUNK,
    STORED,
    CHUNK,
    DELETE,
    DELETED,
    REMOVED,
    FIND_SUCC, // Find the successor for a given key (that is, the node in charge of it)
    RTRN_SUCC,  // Demands a note to set another node as its successor
    CHECK_ACTIVE, // Checks if a node is still active (used in check_predecessor)
    GET_PRED, // Asks for the predecessor of a node (used in stabilize)
    RTRN_PRED, // Returns its predecessor to the node that asked for it (used in stabilize)
    NOTIFY, // Notify our successor that we might be their predecessor (used in stabilize/notify),
    SET_PRED, // Demands the successor to set a new predecessor
    SET_SUCC // Demands the predecessor to set a new successor
}

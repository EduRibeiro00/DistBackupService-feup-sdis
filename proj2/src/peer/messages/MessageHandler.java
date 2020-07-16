package peer.messages;

import peer.protocols.Protocol;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Locale;

/**
 * Class that represents the handling and processing of received messages.
 */
public class MessageHandler {
    private final Protocol protocol; //instance of the protocol
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());

    /**
     * Constructor of the message handler.
     * @param protocol instance of the protocol
     */
    public MessageHandler(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Method that processes a given received message, calling the correct protocol method based on the type of the message.
     * @param buffer containing the message received
     */
    public void process(ByteBuffer buffer) {
        byte[] data = Arrays.copyOfRange(buffer.array(), 0, buffer.position());

        Message message;
        try {
            message = new Message(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Message not recognized: ignoring...");
            return;
        }

        System.out.println(formatter.format(Instant.now()) + " - Received message: " + message.getHeader());

        Header header = message.getHeader();

        // Dispatch message to the protocol's method
        switch (header.getMessageType()) {
            case PUTCHUNK:
            case GIVECHUNK:
                this.protocol.handleBackup(message);
                break;
            case STORED:
                this.protocol.stored(message);
                break;
            case GETCHUNK:
                this.protocol.sendChunk(message);
                break;
            case CHUNK:
                this.protocol.receiveChunk(message);
                break;
            case DELETE:
                this.protocol.delete(message);
                break;
            case REMOVED:
                this.protocol.removed(message);
                break;
            case DELETED:
                this.protocol.receiveDeleted(message);
                break;

                // --- Chord messages ---
            case FIND_SUCC:
                this.protocol.getChord().handleFindSuccessor(header.getKey(), header.getIpAddress(), header.getPort());
                break;
            case RTRN_SUCC:
                this.protocol.getTaskManager().completeTasks(header.getKey(), header.getNode());
                break;
            case GET_PRED:
                this.protocol.getChord().handleGetPred(header.getNode());
                break;
            case RTRN_PRED:
                this.protocol.getChord().handleSetSuccessor(header.getNode());
                break;
            case NOTIFY:
                this.protocol.getChord().handleNotify(header.getNode());
                break;
            case SET_PRED:
                this.protocol.getChord().setPredecessor(header.getNode());
                break;
            case SET_SUCC:
                this.protocol.getChord().setSuccessor(header.getNode());
                break;
            default:
                break;
        }

        this.protocol.receivedHeader(message.getHeader());

    }
}

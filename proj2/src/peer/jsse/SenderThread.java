package peer.jsse;

import peer.jsse.error.OnError;
import peer.messages.Message;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An SSL/TLS client that connects to a peer, using its IP address and port, and sends it a message.
 */
public class SenderThread extends SSLThread implements Runnable {

    /**
     * Sender ID for message sending.
     */
    private static int senderId;

    /**
     * Protocol used for secure communication.
     */
    private static String protocol;

    /**
     * Client keys, used for secure communication.
     */
    private static String clientKeys;

    /**
     * Trust store, used for secure communication.
     */
    private static String trustStore;

    /**
     * Password used to access the client keys and the truststore.
     */
    private static String password;

    /**
     * The remote address of the peer we are trying to connect to.
     */
    private final String remoteAddress;

    /**
     * The port of the peer we are trying to connect to.
     */
    private final int port;

    /**
     * The engine that will be used to encrypt/decrypt data between the peers.
     */
    private final SSLEngine engine;

    /**
     * The socket channel that will be used as the transport link between the peers.
     */
    private SocketChannel socketChannel;

    /**
     * Message to be sent to the other peer.
     */
    private final Message message;

    /**
     * Callable statement to be run on error.
     */
    private final OnError onError;

    /**
     * Formats printed date
     */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());

    /**
     * Executor service to send messages
     */
    private static final ExecutorService service = Executors.newFixedThreadPool(10);

    /**
     * Flag to know if the thread should exit or not.
     */
    private static AtomicBoolean exit = new AtomicBoolean(false);

    /**
     * Static method for message sending.
     * @param remoteAddress IP address to send the message
     * @param port Port to send the message
     * @param message Message to send
     * @param onError Error function to run if error occurs
     */
    public static void sendMessage(String remoteAddress, int port, Message message, OnError onError) {
        try {
            if (!exit.get())
                service.execute(new SenderThread(remoteAddress, port, message, onError));
        } catch (Exception e) {
            System.err.println("Error creating sender thread");
            e.printStackTrace();
        }
    }

    /**
     * Initiates the engine to run as a client using peer information, and allocates space for the
     * buffers that will be used by the engine.
     * @param remoteAddress The IP address of the peer.
     * @param port The peer's port that will be used.
     */
    private SenderThread(String remoteAddress, int port, Message message, OnError onError) throws Exception {
        this.remoteAddress = remoteAddress;
        this.port = port;
        this.message = message;
        this.onError = onError;
        SSLContext context = SSLContext.getInstance(protocol);
        context.init(createKeyManagers(clientKeys, password, password), createTrustManagers(trustStore, password), new SecureRandom());
        this.engine = context.createSSLEngine(remoteAddress, port);
        this.engine.setUseClientMode(true);
    }

    /**
     * Sets the sender id for all the sender threads
     * @param senderId the id of the sender
     */
    public static void setSenderId(int senderId) {
        SenderThread.senderId = senderId;
    }


    /**
     * Sets the protocol to be used to create the SLLContext
     * @param protocol The SSL/TLS protocol to be used. Java 1.6 will only run with up to TLSv1 protocol. Java 1.7 or higher also supports TLSv1.1 and TLSv1.2 protocols.
     */
    public static void setProtocol(String protocol) {
        SenderThread.protocol = protocol;
    }

    /**
     * Setter for the client keys.
     * @param clientKeys the keys of the client path
     */
    public static void setClientKeys(String clientKeys) {
        SenderThread.clientKeys = clientKeys;
    }

    /**
     * Setter for the trust store.
     * @param trustStore the trust store path
     */
    public static void setTrustStore(String trustStore) {
        SenderThread.trustStore = trustStore;
    }

    /**
     * Setter for the client keys password.
     * @param password the password for the client keys and trust store
     */
    public static void setPassword(String password) {
        SenderThread.password = password;
    }

    /**
     * Opens a socket channel to communicate with the configured server and tries to complete the handshake protocol.
     * @return True if client established a connection with the server, false otherwise.
     * @throws Exception when the connection fails
     */
    protected boolean connect() throws Exception {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(remoteAddress, port));
        while(!socketChannel.finishConnect());
        engine.beginHandshake();
        return performHandshake(socketChannel, engine);
    }

    /**
     * Method that performs a write operation, using SSL and secure mechanisms, and sends the message to the other peer.
     * @param socketChannel Socket channel used for writing
     * @param engine Engine used for encryption/decryption of the data
     * @throws IOException when the the sending of the message fails
     */
    protected void writeToPeer(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        SSLSession session = engine.getSession();
        byte[] msg = message.convertToBytes();

        // create byte buffers for communication
        ByteBuffer appData = ByteBuffer.allocate(Math.max(session.getApplicationBufferSize(), msg.length));
        ByteBuffer netData = ByteBuffer.allocate(session.getPacketBufferSize());
        ByteBuffer msgBuf = ByteBuffer.allocate(msg.length + 500);

        // insert message bytes to byte buffer and flip it
        appData.put(msg);
        appData.flip();

        // while there is still data to be sent
        while (appData.hasRemaining()) {
            // use SSL Engine to wrap the data
            netData.clear();
            SSLEngineResult result = engine.wrap(appData, netData);

            // get result status
            switch (result.getStatus()) {
                // everything went OK
                case OK:
                    // flip buffer and write to socket channel
                    netData.flip();
                    msgBuf.put(netData);
                    break;

                // in case of buffer underflow
                case BUFFER_UNDERFLOW:
                    throw new SSLException("Error: underflow after wrap");

                // in case of buffer overflow
                case BUFFER_OVERFLOW:
                    netData = overflowPacketBufferCall(engine, netData);
                    break;

                // when the connection was closed
                case CLOSED:
                    closeConnection(socketChannel, engine);
                    return;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }

        msgBuf.flip();
        socketChannel.write(msgBuf);
    }

    /**
     * Method that explicitly closes the connection.
     * @throws IOException when the connection can't be successfully closed
     */
    protected void shutdown() throws IOException {
        closeConnection(socketChannel, engine);
        executor.shutdown();
    }

    public static void exit() {
        exit.set(true);
        service.shutdown();
        try {
            if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException ex) {
            service.shutdownNow();
        }
    }

    /**
     * Method to be called to run a separate thread that performs all the needed operations
     * to perform the sending of the encrypted message to the other peer. Performs connection,
     * encryption using SSLEngine, writing of the message and shutdown of the connection.
     * In case of any error, runs the error method of the onError class.
     */
    @Override
    public void run() {
        if (message == null)
            return;

        message.setSenderId(senderId);

        try {
            if(!connect()){
                System.err.println("Error (connection) when sending message: " + message.getHeader());
                if (onError != null)
                    onError.errorOccurred();
                return;
            }


            writeToPeer(socketChannel, engine);

            shutdown();
            System.out.println(formatter.format(Instant.now()) + " - Sent message: " + message.getHeader());
        } catch (Exception e) {
            System.err.println(formatter.format(Instant.now()) + " - Error (exception) when sending message: " + message.getHeader());
            e.printStackTrace();
            if (onError != null)
                onError.errorOccurred();
        }
    }
}
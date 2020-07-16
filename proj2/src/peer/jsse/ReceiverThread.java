package peer.jsse;

import peer.messages.MessageHandler;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

/**
 * An SSL/TLS "server" that accepts and reads messages from other peers.
 */
public class ReceiverThread extends SSLThread implements Runnable {
    /**
     * Size of the message buffer.
     */
    private static final int MESSAGE_SIZE = 64500;

    /**
     * ExecutorService responsible for threads.
     */
    private final ExecutorService service;

    /**
     * SSL context that will be used to receive the messages.
     */
    private final SSLContext context;

    /**
     * A part of Java NIO that will be used to serve all connections to the server in one thread.
     */
    private final Selector selector;

    /**
     * Message handler that will process receiving data/messages.
     */
    private final MessageHandler messageHandler;

    /**
     * Flag to know if the thread should exit or not.
     */
    private static AtomicBoolean exit;

    /**
     * Constructor of the receiver thread.
     * @param messageHandler Message handler for processing messages
     * @param protocol The protocol to be used as context
     * @param serverKeys Server keys used
     * @param trustStore Trust store used
     * @param password Password used
     * @param nThreads Number of threads for the executor service
     * @throws Exception
     */
    public ReceiverThread(MessageHandler messageHandler, String protocol, String serverKeys, String trustStore, String password, int nThreads) throws Exception {
        this.service = Executors.newFixedThreadPool(nThreads);
        this.messageHandler = messageHandler;

        context = SSLContext.getInstance(protocol);
        context.init(createKeyManagers(serverKeys, password, password),
                createTrustManagers(trustStore, password), new SecureRandom());
        selector = SelectorProvider.provider().openSelector();
        exit = new AtomicBoolean(false);
    }

    /**
     * Adds a server to the server pool of the selector.
     * @param ipAddress The IP address that of the new server
     * @param port The port number of the new server
     * @throws IOException
     */
    public void addServer(String ipAddress, int port) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(ipAddress, port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Method that accepts a new connection request that arrives the this peer.
     * @param key Key for listening to new connection requests
     * @throws Exception
     */
    private void accept(SelectionKey key) throws Exception {
        // create socket channel and SSL engine
        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();

        // try to perform handshake
        if (performHandshake(socketChannel, engine)) {
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
        } else {
            socketChannel.close();
            System.err.println("Connection closed due to handshake failure.");
        }
    }

    /**
     * Method that will be called when there is data available to be read, i.e. another peer as sent a message.
     * @param socketChannel Socket channel used to receive the message.
     * @param engine SSL engine for the encryption and decryption of messages
     * @return Byte buffer with the message content
     * @throws IOException
     */
    protected ByteBuffer readFromPeer(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        SSLSession session = engine.getSession();

        // create and allocate space for byte buffers
        ByteBuffer message = ByteBuffer.allocate(Math.max(session.getApplicationBufferSize(), MESSAGE_SIZE) + 500);
        ByteBuffer netData = ByteBuffer.allocate(Math.max(session.getPacketBufferSize(), MESSAGE_SIZE) + 500);

        netData.clear();
        // read data from the socket channel
        if (socketChannel.read(netData) > 0) {
            // if bytes were read, flip the buffer
            netData.flip();
            while (netData.hasRemaining()) {
                // unwrap the message content using SSL engine
                SSLEngineResult result = engine.unwrap(netData, message);

                // get engine status
                switch (result.getStatus()) {

                    // everything went OK
                    case OK:
                        break;

                    // in case of buffer underflow
                    case BUFFER_UNDERFLOW:
                        netData = underflowAppBufferCall(engine, netData);
                        break;

                    // in case of buffer overflow
                    case BUFFER_OVERFLOW:
                        message = overflowAppBufferCall(engine, message);
                        break;

                    // when the connection was closed
                    case CLOSED:
                        closeConnection(socketChannel, engine);
                        return message;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }
        } else {
            System.out.println("Received end of stream. Will try to close connection with client...");
            handleEndOfStream(socketChannel, engine);
            return message;
        }

        return message;
    }

    public static void exit() {
        exit.set(true);
    }

    /**
     * Should be called in order for the server to start listening to new connections.
     * This method will run in a loop as long as the server is active
     * and also wake up the listener, which may be in blocking select() state.
     */
    @Override
    public void run() {
        while (!exit.get()) {
            try {
                selector.select();
            } catch (IOException e) {
                continue;
            }
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (!key.isValid()) {
                    continue;
                }

                if (key.isValid() && key.isAcceptable()) {
                    try {
                        accept(key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (key.isValid() && key.isReadable()) {
                    key.cancel();
                    // execute in thread pool the receiving of the data and the
                    // composing and processing of the message
                    this.service.execute(() -> {
                        SocketChannel channel = (SocketChannel) key.channel();
                        SSLEngine engine = (SSLEngine) key.attachment();
                        ByteBuffer message;

                        try {
                            message = readFromPeer(channel, engine);
                        } catch (IOException e) {
                            System.err.println("Error while trying to read a message");
                            e.printStackTrace();
                            return;
                        }

                        this.messageHandler.process(message);

                        try {
                            closeConnection(channel, engine);
                        } catch (IOException ignored) {
                            System.err.println("Error closing the socket after reading a message");
                        }
                    });
                }
            }
        }

       this.service.shutdown();
        try {
            if (!this.service.awaitTermination(60, TimeUnit.SECONDS)) {
                this.service.shutdownNow();
            }
        } catch (InterruptedException ex) {
            this.service.shutdownNow();
        }
    }

}

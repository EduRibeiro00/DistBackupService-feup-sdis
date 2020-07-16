package peer.jsse;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base abstract class that contains the foundations for secure SSL communication between peers.
 */
public abstract class SSLThread {

    /**
     * Will be used to execute tasks that may emerge during handshake in parallel with the server's main thread.
     */
    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Implements the handshake protocol between two peers, required for the establishment of the SSL/TLS connection.
     * @param socketChannel The socket channel that connects the two peers.
     * @param engine The engine that will be used for encryption/decryption of the data exchanged with the other peer.
     * @return True if the connection handshake was successful or false if an error occurred.
     * @throws IOException
     */
    protected boolean performHandshake(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        SSLEngineResult result;
        SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
        final int additionalSpace = 50;
        int appBufferSize = engine.getSession().getApplicationBufferSize();

        ByteBuffer dataApp = ByteBuffer.allocate(appBufferSize + additionalSpace);
        ByteBuffer dataNet = ByteBuffer.allocate(appBufferSize + additionalSpace);
        ByteBuffer dataPeerApp = ByteBuffer.allocate(appBufferSize + additionalSpace);
        ByteBuffer dataPeerNet = ByteBuffer.allocate(appBufferSize + additionalSpace);

        while (status != SSLEngineResult.HandshakeStatus.FINISHED && status != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            // ----- Status: Need unwrap -----
            if (status == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                // if we cannot read from socket channel
                if (socketChannel.read(dataPeerNet) < 0) {

                    // if outbound and inbound are both done, handshake failed
                    if (engine.isOutboundDone() && engine.isInboundDone()) {
                        return false;
                    }

                    // try to close inbound
                    try {
                        engine.closeInbound();
                    } catch (SSLException e) {
                        System.err.println("Error when closing inbound");
                        e.printStackTrace();
                    }

                    // close outbound and get status for next iteration
                    engine.closeOutbound();
                    status = engine.getHandshakeStatus();
                    continue;
                }

                // after reading to the byte buffer, flip it
                dataPeerNet.flip();

                // perform unwrap
                try {
                    result = engine.unwrap(dataPeerNet, dataPeerApp);
                    dataPeerNet.compact();
                    status = result.getHandshakeStatus();
                } catch (SSLException e) {
                    System.err.println("Error unwrapping");
                    e.printStackTrace();
                    engine.closeOutbound();
                    status = engine.getHandshakeStatus();
                    continue;
                }

                // get result status
                switch (result.getStatus()) {
                    // everything went OK
                    case OK:
                        break;

                    // in case of buffer underflow
                    case BUFFER_UNDERFLOW:
                        dataPeerNet = underflowAppBufferCall(engine, dataPeerNet);
                        break;

                    // in case of buffer overflow
                    case BUFFER_OVERFLOW:
                        dataPeerApp = overflowAppBufferCall(engine, dataPeerApp);
                        break;

                    // when the connection was closed
                    case CLOSED:
                        // if outbound done, handshake failed
                        if (engine.isOutboundDone()) {
                            return false;
                        } else {
                            engine.closeOutbound();
                            status = engine.getHandshakeStatus();
                            break;
                        }
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }


            // ----- Status: Need wrap -----
            } else if (status == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                dataNet.clear();
                // perform wrap and extract result
                try {
                    result = engine.wrap(dataApp, dataNet);
                    status = result.getHandshakeStatus();
                } catch (SSLException e) {
                    System.err.println("Error wrapping.");
                    e.printStackTrace();
                    engine.closeOutbound();
                    status = engine.getHandshakeStatus();
                    continue;
                }

                // get result status
                switch (result.getStatus()) {
                    // everything went OK
                    case OK:
                        // flip the buffer and perform write on socket channel
                        dataNet.flip();
                        while (dataNet.hasRemaining()) {
                            socketChannel.write(dataNet);
                        }
                        break;

                    // in case of buffer underflow
                    case BUFFER_UNDERFLOW:
                        // exception, because an underflow should not occurs after a wrap...
                        throw new SSLException("Error: underflow after wrap");

                    // in case of buffer overflow
                    case BUFFER_OVERFLOW:
                        dataNet = overflowPacketBufferCall(engine, dataNet);
                        break;

                    // when the connection was closed
                    case CLOSED:
                        // flip the buffer and perform write on socket channel
                        try {
                            dataNet.flip();
                            while (dataNet.hasRemaining()) {
                                socketChannel.write(dataNet);
                            }
                            dataPeerNet.clear();
                        } catch (Exception e) {
                            System.err.println("Failure in socket channel");
                            status = engine.getHandshakeStatus();
                        }
                        break;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }



            // ----- Status: Need task -----
            } else if (status == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                // get delegated task from engine and execute it
                Runnable task;
                while ((task = engine.getDelegatedTask()) != null) {
                    executor.execute(task);
                }

                // close outbound and get status for next iteration
                status = engine.getHandshakeStatus();

            // ----- Status: Other status -----
            } else if ((status != SSLEngineResult.HandshakeStatus.FINISHED) && (status != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)) {
                throw new IllegalStateException("Invalid state in SSL : " + status);
            }
        }

        // handshake completed!
        return true;
    }


    /**
     * Method that is called when an overflow occurs on the application buffer.
     * @param engine SSL engine used
     * @param buffer Byte buffer used
     * @return New byte buffer
     */
    protected ByteBuffer overflowAppBufferCall(SSLEngine engine, ByteBuffer buffer) {
        return bufferEnlarge(buffer, engine.getSession().getApplicationBufferSize());
    }

    /**
     * Method that is called when an underflow occurs on the application buffer.
     * @param engine SSL engine used
     * @param buffer Byte buffer used
     * @return New byte buffer
     */
    protected ByteBuffer underflowAppBufferCall(SSLEngine engine, ByteBuffer buffer) {
        if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
            return buffer;
        } else {
            ByteBuffer replaceBuffer = overflowPacketBufferCall(engine, buffer);
            buffer.flip();
            replaceBuffer.put(buffer);
            return replaceBuffer;
        }
    }

    /**
     * Method that is called when an overflow occurs on the packet buffer.
     * @param engine SSL engine used
     * @param buffer Byte buffer used
     * @return New byte buffer
     */
    protected ByteBuffer overflowPacketBufferCall(SSLEngine engine, ByteBuffer buffer) {
        return bufferEnlarge(buffer, engine.getSession().getPacketBufferSize());
    }

    /**
     * Method that tries to enlarge the buffer that was passed as an argument. If the session proposed
     * capacity is higher than the current buffer capacity, it allocates that space to the buffer;
     * otherwise the buffer gets twice its previous capacity.
     * @param buffer Buffer to be enlarged
     * @param sessionProposedCapacity Capacity proposed by the session
     * @return New buffer, possibly enlarged
     */
    protected ByteBuffer bufferEnlarge(ByteBuffer buffer, int sessionProposedCapacity) {
        if (sessionProposedCapacity > buffer.capacity()) {
            buffer = ByteBuffer.allocate(sessionProposedCapacity);
        } else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }
        return buffer;
    }

    /**
     * Method called to close the connection.
     * @param socketChannel Socket channel being used
     * @param engine SSL engine being used
     * @throws IOException
     */
    protected void closeConnection(SocketChannel socketChannel, SSLEngine engine) throws IOException  {
        engine.closeOutbound();
        performHandshake(socketChannel, engine);
        socketChannel.close();
    }

    /**
     * Method that handles the end of stream.
     * @param socketChannel Socket channel being used
     * @param engine SSL engine being used
     * @throws IOException
     */
    protected void handleEndOfStream(SocketChannel socketChannel, SSLEngine engine) throws IOException  {
        try {
            engine.closeInbound();
        } catch (Exception e) {
            System.err.println("Error closing inbound");
        }
        // close the connection
        closeConnection(socketChannel, engine);
    }

    /**
     * Method that creates the necessary trust managers to initiate the SSLContext. Uses JSK keystore.
     * @param filepath Path to JSK keystore
     * @param keystorePassword Password of the keystore
     * @return Array with trust managers that will be used in the connection
     * @throws Exception
     */
    protected TrustManager[] createTrustManagers(String filepath, String keystorePassword) throws Exception {
        // get and load keystore
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream trustStoreIS = new FileInputStream(filepath)) {
            trustStore.load(trustStoreIS, keystorePassword.toCharArray());
        }

        // get trust manager factory and extract the trust managers from it
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(trustStore);
        return trustFactory.getTrustManagers();
    }

    /**
     * Method that creates the necessary key managers to initiate the SSLContext. Uses JSK keystore.
     * @param filepath Path to JSK keystore
     * @param keystorePassword Password of the keystore
     * @param keyPassword Password of the key
     * @return Array with the keys that will be used in the connection
     * @throws Exception
     */
    protected KeyManager[] createKeyManagers(String filepath, String keystorePassword, String keyPassword) throws Exception {
        // get and load keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream keyStoreIS = new FileInputStream(filepath)) {
            keyStore.load(keyStoreIS, keystorePassword.toCharArray());
        }

        // get key manager factory and extract keys from it
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyPassword.toCharArray());
        return kmf.getKeyManagers();
    }
}

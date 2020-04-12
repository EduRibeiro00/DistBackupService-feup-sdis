package peer;

import peer.messages.MessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class that represents a thread that is always receiving and processing messages, for a specific multicast channel.
 */
public class ReceiverThread implements Runnable {
    private MessageHandler messageHandler;      /** messageHandler that will process receiving data */
    private MulticastSocket mCastSkt;           /** multicast socket to receive data */
    private int bufSize;                        /** buffer size */
    private ExecutorService service;            /** ExecutorService responsible for threads */

    /**
     * Constructor of the receiver thread.
     * @param messageHandler message handler for processing messages
     * @param ipAddress IP address for the multicast socket
     * @param port port for the multicast socket
     * @param bufSize buffer size
     * @param nThreads number of threads for the executor service
     * @throws IOException
     */
    public ReceiverThread(MessageHandler messageHandler, String ipAddress, int port, int bufSize, int nThreads) throws IOException {
        this.messageHandler = messageHandler;

        this.mCastSkt = new MulticastSocket(port);
        this.mCastSkt.joinGroup(InetAddress.getByName(ipAddress));
        this.mCastSkt.setTimeToLive(1);

        this.bufSize = bufSize;
        this.service = Executors.newFixedThreadPool(nThreads);
    }

    /**
     * Override of the thread run method. Is always receiving new messages and dispatching them using the thread pool.
     */
    @Override
    public void run() {
        System.out.println("Thread ready for receiving packets");

        // will read forever until peer is closed
        while (true) {
            byte[] buf = new byte[this.bufSize];
            DatagramPacket packet = new DatagramPacket(buf, this.bufSize);

            try {
                this.mCastSkt.receive(packet);

                this.handleMessage(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends the packet content to the peer for processing.
     * @param packet the packet received for processing
     */
    private void handleMessage(DatagramPacket packet) {

        Runnable processMessage = () -> this.messageHandler.process(packet);

        this.service.execute(processMessage);
    }
}

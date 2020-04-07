package peer;

import peer.messages.MessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiverThread implements Runnable {
    private MessageHandler messageHandler;      // messageHandler that will process receiving data
    private MulticastSocket mCastSkt;           // multicast socket to receive data
    private int bufSize;                        // buffer size
    private ExecutorService service;            // ExecutorService responsible for threads

    public ReceiverThread(MessageHandler messageHandler, String ipAddress, int port, int bufSize, int nThreads) throws IOException {
        this.messageHandler = messageHandler;

        this.mCastSkt = new MulticastSocket(port);
        this.mCastSkt.joinGroup(InetAddress.getByName(ipAddress));
        this.mCastSkt.setTimeToLive(1);

        this.bufSize = bufSize;
        this.service = Executors.newFixedThreadPool(nThreads);
    }

    @Override
    public void run() {
        System.out.println("Thread ready for receiving packets");
        byte[] buf = new byte[this.bufSize];
        DatagramPacket packet = new DatagramPacket(buf, this.bufSize);

        // will read forever until peer is closed
        while (true) {
            try {
                this.mCastSkt.receive(packet);

                this.handleMessage(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends the packet content to the peer for processing
     * @param packet
     */
    private void handleMessage(DatagramPacket packet) {
        byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

        Runnable processMessage = () -> this.messageHandler.process(data);

        this.service.execute(processMessage);
    }
}

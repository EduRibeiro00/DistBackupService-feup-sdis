package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiveThread implements Runnable {
    private MessageHandler messageHandler;          // messageHandler that will process receiving data
    private MulticastSocket mCastSkt;               // multicast socket to receive data
    private int bufSize;
    private ExecutorService service;                // ExecutorService responsible for threads

    public ReceiveThread(MessageHandler messageHandler, MulticastSocket mCastSkt, int bufSize, int nThreads) {
        this.messageHandler = messageHandler;
        this.mCastSkt = mCastSkt;
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

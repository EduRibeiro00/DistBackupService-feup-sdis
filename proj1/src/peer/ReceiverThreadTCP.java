package peer;

import peer.messages.Message;
import peer.messages.MessageType;
import peer.protocols.Protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiverThreadTCP implements Runnable {
    private Protocol protocol;                  // protocol 2
    private ServerSocket serverSocket;          // multicast socket to receive data
    private int bufSize;                        // buffer size
    private ExecutorService service;            // ExecutorService responsible for threads

    public ReceiverThreadTCP(Protocol protocol, int port, int bufSize, int nThreads) throws IOException {
        this.protocol = protocol;

        this.serverSocket = new ServerSocket(port);
        this.serverSocket.setSoTimeout(0);

        this.bufSize = bufSize;
        this.service = Executors.newFixedThreadPool(nThreads);
    }

    @Override
    public void run() {
        System.out.println("TCP Thread ready for receiving messages");

        // will read forever until peer is closed
        while (true) {

            try {
                Socket chunkSocket = serverSocket.accept();

                this.handleMessage(chunkSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handleMessage(Socket chunkSocket) {

        Runnable processTCP = () -> this.receiveChunkTCP(chunkSocket);

        this.service.execute(processTCP);
    }


    private void receiveChunkTCP(Socket socket) {
        try {
            // open streams
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

            // read
            byte[] msgContent = new byte[this.bufSize];
            int read_size = in.read(msgContent, 0, this.bufSize);

            // close streams
            socket.shutdownOutput();
            while(in.read() != -1);
            out.close();
            in.close();

            // close socket
            socket.close();

            if(read_size == -1)
                return;

            Message msg = new Message(Arrays.copyOfRange(msgContent, 0, read_size));

            System.out.println("Received message (TCP): " + msg.getHeader());

            if(msg.getHeader().getMessageType() == MessageType.CHUNK) {
                this.protocol.receiveChunk(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

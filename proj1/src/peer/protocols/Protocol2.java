package peer.protocols;

import peer.messages.Header;
import peer.messages.Message;
import peer.messages.MessageType;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Protocol2 extends Protocol1 {

    public Protocol2(int peerID, String ipAddressMC, int portMC, String ipAddressMDB, int portMDB, String ipAddressMDR, int portMDR) {
        super(peerID, ipAddressMC, portMC, ipAddressMDB, portMDB, ipAddressMDR, portMDR);
        this.setVersion("1.1");
    }

    @Override
    public void initiateRestore(String filepath) {
        // get the file ID of the chunk
        String fileId = this.fileManager.getHashForFile(filepath);
        if(fileId == null) {
            System.err.println("Unknown filepath given");
            return;
        }

        // get the number of chunks this file was divided in
        int maxNumChunks = this.fileManager.getMaxChunkNo(fileId);
        if (maxNumChunks == -1) {
            System.err.println("No information for this file's chunks");
            return;
        }

        // extract filename from filepath
        String filename = Paths.get(filepath).getFileName().toString();

        // create new file restorer
        this.chunkManager.createFileRestorer(filename, fileId, maxNumChunks);

        // send a GETCHUNK for each chunk of the file and initiate a TCP connection
        for (int i = 0; i <= maxNumChunks; i++) {

            try {
                executor.schedule(() -> receiveChunkTCP(this.portMDR, fileId, i), 0, TimeUnit.SECONDS);
                new Message(this.protocolVersion, MessageType.GETCHUNK, this.peerID, fileId, i).send(
                        this.ipAddressMC, this.portMC

                );
            }
            catch (IOException e) {
                System.err.println("Error sending message");
                e.printStackTrace();
            }
        }
    }

    private void receiveChunkTCP(int port, String fileId, int chunkNo) {
        // open socket
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(this.TIMEOUT);

            Socket socket = serverSocket.accept();

            // open streams
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

            // read
            byte[] msgContent = new byte[64500];
            int read_size = in.read(msgContent, 0, 64500);

            if(read_size == -1)
                return;

            // close streams
            socket.shutdownOutput();
            while(in.read() != 0);
            out.close();
            in.close();

            // close sockets
            socket.close();
            serverSocket.close();

            Message msg = new Message(Arrays.copyOfRange(msgContent, 0, read_size));
            if(!msg.getHeader().getFileId().equals(fileId) || msg.getHeader().getChunkNo() != chunkNo)
                return;

            this.chunkManager.insertChunkForRestore(msg.getHeader().getFileId(), msg.getHeader().getChunkNo(), msg.getBody());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void sendChunk(Message message) {
        Header header = message.getHeader();
        String fileId = header.getFileId();
        int chunkNo = header.getChunkNo();

        // if the peer has that chunk saved
        if (this.fileManager.isChunkStored(fileId, chunkNo)) {
            byte[] chunkContent;

            try {
                chunkContent = this.fileManager.getChunk(fileId, chunkNo);


            } catch (IOException e) {
                e.printStackTrace(); // TODO: change this
                return;
            }

        switch (header.getVersion()) {
            case "1.1":

                Socket socket = null;
                try {
                    // open socket
                    socket = new Socket(message.getIpAddress(), message.getPort());

                    // open streams
                    BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                    BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

                    // write
                    out.write(chunkContent, 0, chunkContent.length);
                    out.flush();

                    // close streams
                    socket.shutdownOutput();
                    while(in.read() != 0);
                    out.close();
                    in.close();

                    // close socket
                    socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;

            case "1.0":
                    MulticastSocket mCastSkt;

                    long waitTime = new Random().nextInt(401);
                    try {
                        mCastSkt = new MulticastSocket(this.portMDR);
                        mCastSkt.joinGroup(InetAddress.getByName(this.ipAddressMDR));
                        mCastSkt.setTimeToLive(1);
                        mCastSkt.setSoTimeout((int) waitTime);
                    } catch (IOException e) {
                        e.printStackTrace(); // TODO: change this
                        return;
                    }

                    byte[] buffer = new byte[64500];
                    DatagramPacket packet = new DatagramPacket(buffer, 64500);
                    try {
                        while(waitTime > 0) {
                            long before = System.currentTimeMillis();
                            mCastSkt.receive(packet);
                            waitTime -= System.currentTimeMillis() - before;

                            Message msg = new Message(packet.getData());
                            if(msg.getHeader().getMessageType() == MessageType.CHUNK &&
                                    msg.getHeader().getFileId().equals(fileId) &&
                                    msg.getHeader().getChunkNo() == chunkNo) {
                                return;
                            }
                            mCastSkt.setSoTimeout((int) waitTime);
                        }
                    } catch (IOException ignore) {}

                    // send message with chunk
                    try {
                        new Message(this.protocolVersion, MessageType.CHUNK, this.peerID, fileId, chunkNo, chunkContent).send(
                                this.ipAddressMDR, this.portMDR
                        );
                    } catch (IOException e) {
                        System.err.println("Error sending chunk message");
                        e.printStackTrace();
                    }

                    break;
            }


    }

 }

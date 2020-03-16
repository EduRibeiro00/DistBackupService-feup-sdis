package peer;

import java.util.Arrays;


public class Message {
    final String crlf = "\r\n";
    Header header;
    byte[] body;

    /**
     * Constructor for message receiving
     * @param data byte array with received data
     * @throws Exception
     */
    public Message(byte[] data) throws Exception {
        String message = Arrays.toString(data);
        String[] split = message.split(this.crlf);

        if (split.length != 2){
            throw new Exception("Invalid message received");
        }

        this.header = new Header(Arrays.asList(split[0].split(" ")));
        this.body = split[1].getBytes();
    }

    /**
     * Fills the Message class for message sending
     * @param version the version of the protocol to be used
     * @param msgType the type of message to be sent
     * @param senderId the ID of the message sender
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param repDeg the desired replication degree of the file's chunk (may be unused)
     */
    public Message(String version, MessageType msgType, String senderId, String fileId, int chunkNo, int repDeg, byte[] body) {
        this.header = new Header(version, msgType, senderId, fileId, chunkNo, repDeg);
        this.body = body;
    }
}

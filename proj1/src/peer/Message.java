package peer;

import java.util.Arrays;


public class Message {
    final String crlf = String.valueOf(0xD) + 0xA;
    Header header;
    byte[] body;

    public Message(byte[] data) throws Exception {
        String message = Arrays.toString(data);
        String[] split = message.split(this.crlf);

        if (split.length != 2){
            throw new Exception("Invalid message received");
        }

        this.header = new Header(Arrays.asList(split[0].split(" ")));
        this.body = split[1].getBytes();
    }
}

package peer;

import java.io.Serializable;

public class AddressRecord implements Serializable {
    private String ipAddress;
    private int port;

    public AddressRecord(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }
}

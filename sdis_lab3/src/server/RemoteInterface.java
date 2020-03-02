package server;

import java.rmi.*;

public interface RemoteInterface extends Remote {
    String register(String dnsName, String ipAddress) throws RemoteException;
    String lookup(String dnsName) throws RemoteException;
}

package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface that represents the remote interface that each peer offers to a client.
 */
public interface RemoteInterface extends Remote {
    String test(String testString) throws RemoteException;
    int backup(String fileId, int chunkNo, byte[] fileContent, int replicationDeg) throws RemoteException;
}

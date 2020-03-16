package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface that represents the remote interface that each peer offers to a client.
 */
public interface RemoteInterface extends Remote {
    // TODO: mudar os argumentos de cada protocolo

    /**
     * Backup request.
     * Sends a backup message for peer-peer communication
     * @param version the version of the protocol to be used
     * @param fileId the file identifier in the backup service, as the result of SHA256
     * @param chunkNo the chunk number of the specified file (may be unsued)
     * @param fileContent the body of the message
     * @param replicationDeg the desired replication degree of the file's chunk (may be unused)
     * @throws RemoteException
     */
    int backup(String version, String fileId, int chunkNo, String fileContent, int replicationDeg) throws RemoteException;

    /**
     * Restore request.
     * @param testString
     * @return
     * @throws RemoteException
     */
    String restore(String testString) throws RemoteException;

    /**
     * Delete request.
     * @param testString
     * @return
     * @throws RemoteException
     */
    String delete(String testString) throws RemoteException;

    /**
     * Reclaim request.
     * @param testString
     * @return
     * @throws RemoteException
     */
    String reclaim(String testString) throws RemoteException;

    /**
     * State request.
     * @param testString
     * @return
     * @throws RemoteException
     */
    String state(String testString) throws RemoteException;
}

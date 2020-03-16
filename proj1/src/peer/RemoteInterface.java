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
     * @param testString
     * @return
     * @throws RemoteException
     */
    String backup(String testString) throws RemoteException;

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

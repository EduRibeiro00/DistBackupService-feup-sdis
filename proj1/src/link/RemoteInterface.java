package link;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Interface that represents the remote interface that each peer offers to a client.
 */
public interface RemoteInterface extends Remote {
    /**
     * Backup request.
     * @param filepath filepath of the file we want to backup
     * @param replicationDegree desired replication factor for the file's chunks
     * @throws RemoteException
     */
    void backup(String filepath, int replicationDegree) throws RemoteException;


    /**
     * Restore request.
     * @param filepath filepath of the file we want to backup
     * @throws RemoteException
     */
    void restore(String filepath) throws RemoteException;


    /**
     * Delete request.
     * @param filepath filepath of the file we want to backup
     * @throws RemoteException
     */
    void delete(String filepath) throws RemoteException;


    /**
     * Reclaim request.
     * @param diskSpace maximum of disk space we want to reclaim
     * @throws RemoteException
     */
    void reclaim(int diskSpace) throws RemoteException;


    /**
     * State request.
     * @return String with peer state
     * @throws RemoteException
     */
    String state() throws RemoteException;
}

package peer;

import link.RemoteInterface;

import java.rmi.RemoteException;

/**
 * Class that makes the connection between the peer and the testing client
 */
public class PeerTestLink implements RemoteInterface {

    private Peer peer;

    /**
     * Constructor of the class
     * @param peer peer that is going to fulfill the requests that the link will send
     */
    public PeerTestLink(Peer peer) {
        this.peer = peer;
    }


    /**
     * Implementation of the backup request.
     * @param filepath filepath of the file we want to backup
     * @param replicationFactor desired replication factor for the file's chunks
     * @throws RemoteException
     */
    @Override
    public void backup(String filepath, int replicationFactor) throws RemoteException {

    }


    /**
     * Implementation of the delete request.
     * @param filepath filepath of the file we want to delete
     * @throws RemoteException
     */
    @Override
    public void delete(String filepath) throws RemoteException {

    }


    /**
     * Implementation of the restore request.
     * @param filepath filepath of the file we want to restore
     * @throws RemoteException
     */
    @Override
    public void restore(String filepath) throws RemoteException {

    }


    /**
     * Implementation of the reclaim request.
     * @param diskSpace maximum of disk space we want to reclaim
     * @throws RemoteException
     */
    @Override
    public void reclaim(int diskSpace) throws RemoteException {

    }


    /**
     * Implementation of the state request.
     * @return String with peer state
     * @throws RemoteException
     */
    @Override
    public String state() throws RemoteException {
        return null;
    }
}

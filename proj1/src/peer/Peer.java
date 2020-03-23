package peer;

import link.RemoteInterface;
import peer.messages.MessageHandler;
import peer.protocols.Protocol;
import peer.protocols.Protocol1;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;

/**
 * Class that makes the connection between the peer and the testing client
 */
public class Peer implements RemoteInterface {
    private final static int N_THREADS_PER_CHANNEL = 10;    // number of threads ready for processing packets in each channel
    private final static int BUFFER_SIZE_CONTROL = 500;     // buffer size for messages received in the control socket
    private final static int BUFFER_SIZE = 64500;           // buffer size for messages received in the control socket

    private Protocol protocol;  // protocol responsible for the peer behaviours


    /**
     * Constructor of the peer
     * @param ipAddressMC IP Address of the MC channel
     * @param portMC Port of the MC channel
     * @param ipAddressMDB IP Address of the MDB channel
     * @param portMDB Port of the MDB channel
     * @param ipAddressMDR IP Address of the MDR channel
     * @param portMDR Port of the MDR channel
     * @param protocolVersion Protocol version of the peer
     * @param peerID Identifier of the peer
     */
    public Peer(String ipAddressMC, int portMC, String ipAddressMDB, int portMDB, String ipAddressMDR, int portMDR, String protocolVersion, int peerID) throws Exception {
        MulticastSocket mCastControl = new MulticastSocket(portMC);
        mCastControl.joinGroup(InetAddress.getByName(ipAddressMC));
        mCastControl.setTimeToLive(1);
        System.out.println("MC channel up!");

        MulticastSocket mCastBackup = new MulticastSocket(portMDB);
        mCastBackup.joinGroup(InetAddress.getByName(ipAddressMDB));
        mCastBackup.setTimeToLive(1);
        System.out.println("MDB channel up!");

        MulticastSocket mCastRestore = new MulticastSocket(portMDR);
        mCastRestore.joinGroup(InetAddress.getByName(ipAddressMDR));
        mCastRestore.setTimeToLive(1);
        System.out.println("MDR channel up!");


        switch (protocolVersion) {
            case "1.0":
                this.protocol = new Protocol1(mCastControl, mCastBackup, mCastRestore, peerID);
                break;
            default:
                throw new Exception(String.format("Version %s not available", protocolVersion));
        }

        System.out.println("Started protocol...");

        MessageHandler messageHandler = new MessageHandler(this.protocol);

        ReceiverThread controlThread = new ReceiverThread(messageHandler, mCastControl, BUFFER_SIZE_CONTROL, N_THREADS_PER_CHANNEL);
        ReceiverThread backupThread = new ReceiverThread(messageHandler, mCastBackup, BUFFER_SIZE, N_THREADS_PER_CHANNEL);
        ReceiverThread restoreThread = new ReceiverThread(messageHandler, mCastRestore, BUFFER_SIZE, N_THREADS_PER_CHANNEL);

        new Thread(controlThread).start();
        new Thread(backupThread).start();
        new Thread(restoreThread).start();

        System.out.println("Started all threads...");
    }


    /**
     * Implementation of the backup request.
     * @param filepath filepath of the file we want to backup
     * @param replicationDegree desired replication factor for the file's chunks
     * @throws RemoteException
     */
    @Override
    public void backup(String filepath, int replicationDegree) {
        if (filepath == null || replicationDegree < 1) {
            throw new IllegalArgumentException("Invalid arguments for backup!");
        }

        // TODO
    }


    /**
     * Implementation of the delete request.
     * @param filepath filepath of the file we want to delete
     * @throws RemoteException
     */
    @Override
    public void delete(String filepath) {

    }


    /**
     * Implementation of the restore request.
     * @param filepath filepath of the file we want to restore
     * @throws RemoteException
     */
    @Override
    public void restore(String filepath) {

    }


    /**
     * Implementation of the reclaim request.
     * @param diskSpace maximum of disk space we want to reclaim
     * @throws RemoteException
     */
    @Override
    public void reclaim(int diskSpace) {

    }


    /**
     * Implementation of the state request.
     * @return String with peer state
     * @throws RemoteException
     */
    @Override
    public String state() {
        return null;
    }
}

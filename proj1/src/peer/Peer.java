package peer;

import link.RemoteInterface;
import peer.protocols.Protocol;
import peer.protocols.Protocol1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;

/**
 * Class that makes the connection between the peer and the testing client
 */
public class Peer implements RemoteInterface {

    private MulticastSocket mCastControl;                   // multicast socket to send control messages
    private MulticastSocket mCastBackup;                    // multicast socket to backup file chunk data
    private MulticastSocket mCastRestore;                   // multicast socket to restore file chunk data
    private String peerID;                                  // identifier of the peer
    private String protocolVersion;                         // protocol version that is being used

    private Protocol protocol;                              // protocol responsible for

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
        this.mCastControl = new MulticastSocket(portMC);
        this.mCastControl.joinGroup(InetAddress.getByName(ipAddressMC));
        this.mCastControl.setTimeToLive(1);
        System.out.println("MC channel up!");

        this.mCastBackup = new MulticastSocket(portMDB);
        this.mCastBackup.joinGroup(InetAddress.getByName(ipAddressMDB));
        this.mCastBackup.setTimeToLive(1);
        System.out.println("MDB channel up!");

        this.mCastRestore = new MulticastSocket(portMDR);
        this.mCastRestore.joinGroup(InetAddress.getByName(ipAddressMDR));
        this.mCastRestore.setTimeToLive(1);
        System.out.println("MDR channel up!");

        this.protocolVersion = protocolVersion;
        this.peerID = String.valueOf(peerID);

        switch (protocolVersion) {
            case "1.0":
                this.protocol = new Protocol1();
            default:
                throw new Exception(String.format("Version %s not available", protocolVersion));
        }
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

package peer;

import link.RemoteInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Class that has the function to create and launch a peer.
 */
public class PeerLauncher {
    /**
     * Main of the peer program.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // check arguments
        if (args.length != 11 && args.length != 13) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        // parse arguments
        String serviceAccessPoint = args[0]; // service access point for RMI
        String ipAddress = args[1]; // IP address of the node
        int portMC = Integer.parseInt(args[2]); // Port number for the MC channel
        int portMDB = Integer.parseInt(args[3]); // Port number for the MDB channel
        int portMDR = Integer.parseInt(args[4]); // Port number for the MDR channel
        int portChord = Integer.parseInt(args[5]); // Port number for the Chord channel
        String protocol = args[6]; // String with the protocol
        String serverKeys = args[7]; // Server keys (for secure communication)
        String clientKeys = args[8]; // Client keys (for secure communication)
        String trustStore = args[9]; // Trust store (for secure communication)
        String password = args[10]; // Password (for secure communication)

        String initIpAddress = null; // IP address of the node we should contact in order to join the Chord ring (if null, we are the initiator)
        int initPort = -1; // Chord port number of the node we should contact in order to join the Chord ring (if -1, we are the initiator)
        if (args.length == 13) {
            initIpAddress = args[11];
            initPort = Integer.parseInt(args[12]);
        }

        try {
            Peer peerObj = new Peer(ipAddress, portMC, portMDB, portMDR, portChord, protocol, serverKeys, clientKeys, trustStore, password, initIpAddress, initPort);

            RemoteInterface remoteObject = (RemoteInterface) UnicastRemoteObject.exportObject(peerObj, 0);
            Registry rmiReg = LocateRegistry.getRegistry();
            rmiReg.rebind(serviceAccessPoint, remoteObject);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}

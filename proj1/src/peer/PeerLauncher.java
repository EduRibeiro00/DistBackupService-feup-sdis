package peer;

import link.RemoteInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Class that has the function to create and launch a peer
 */
public class PeerLauncher {

    public static void main(String[] args) {
        // check arguments
        if (args.length != 9) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        // parse arguments
        String protocolVersion = args[0];
        int peerID = Integer.parseInt(args[1]);
        String serviceAccessPoint = args[2];
        String ipAddressMC = args[3];
        int portMC = Integer.parseInt(args[4]);
        String ipAddressMDB = args[5];
        int portMDB = Integer.parseInt(args[6]);
        String ipAddressMDR = args[7];
        int portMDR = Integer.parseInt(args[8]);


        try {
            ServerInitiator serverInitiator = new ServerInitiator(ipAddressMC, portMC, ipAddressMDB, portMDB, ipAddressMDR, portMDR, protocolVersion, peerID);
            Peer peerLinkObj = new Peer(serverInitiator);


            RemoteInterface remoteObject = (RemoteInterface) UnicastRemoteObject.exportObject(peerLinkObj, 0);
            Registry rmiReg = LocateRegistry.getRegistry();
            rmiReg.bind(serviceAccessPoint, remoteObject);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}

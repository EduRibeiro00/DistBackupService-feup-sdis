package peer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Class that represents the connection that the peer has with the client testing app
 */
public class TestServer {

    public static void main(String[] args) {
        // check arguments
        if (args.length != 9) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        // parse arguments
        String ipAddressMC = args[0];
        int portMC = Integer.parseInt(args[1]);
        String ipAddressMDB = args[2];
        int portMDB = Integer.parseInt(args[3]);
        String ipAddressMDR = args[4];
        int portMDR = Integer.parseInt(args[5]);
        String protocolVersion = args[6];
        int peerID = Integer.parseInt(args[7]);
        String serviceAccessPoint = args[8];


        try {
            Peer peerObj = new Peer(ipAddressMC, portMC, ipAddressMDB, portMDB, ipAddressMDR, portMDR, protocolVersion, peerID);

            RemoteInterface remoteObject = (RemoteInterface) UnicastRemoteObject.exportObject(peerObj, 0);
            Registry rmiReg = LocateRegistry.getRegistry();
            rmiReg.bind(serviceAccessPoint, remoteObject);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}

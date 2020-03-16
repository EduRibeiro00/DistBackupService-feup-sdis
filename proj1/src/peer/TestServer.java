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
        if (args.length != 1) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        String remoteObjectName = args[0];
        Peer peerObj = new Peer();

        try {
            RemoteInterface remoteObject = (RemoteInterface) UnicastRemoteObject.exportObject(peerObj, 0);
            Registry rmiReg = LocateRegistry.getRegistry();
            rmiReg.bind(remoteObjectName, remoteObject);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}

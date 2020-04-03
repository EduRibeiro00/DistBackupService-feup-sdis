package client;

import link.RemoteInterface;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// java client.TestApp 1234 STATE
// java client.TestApp 1234 BACKUP peer/files/ola.txt 1
// java client.TestApp 1234 DELETE peer/files/ola.txt
// java peer.PeerLauncher 1.0 1234 1234 225.0.0.1 8080 225.0.0.1 8081 225.0.0.1 8082
// java peer.PeerLauncher 1.0 5678 5678 225.0.0.1 8080 225.0.0.1 8081 225.0.0.1 8082


/**
 * Class that represents the client that can communicate with a peer and test its services
 */
public class TestApp {
    /**
     * Main method.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // check arguments
        if (args.length > 4) {
            System.err.println("Invalid number of arguments, correct usage:\njava TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            System.exit(1);
        }

        String peerAP = args[0];

        try{
            Registry registry = LocateRegistry.getRegistry();
            RemoteInterface server = (RemoteInterface) registry.lookup(peerAP);

            switch (args[1]) {
                case "BACKUP":
                    System.out.println(String.format("Requesting backup of file: %s with a replication degree of %d",
                            args[2], Integer.parseInt(args[3])));
                    server.backup(args[2], Integer.parseInt(args[3]));
                    break;
                case "RESTORE":
                    System.out.println(String.format("Requesting restoration of file: %s", args[2]));
                    server.restore(args[2]);
                    break;
                case "DELETE":
                    System.out.println(String.format("Requesting deletion of file: %s", args[2]));
                    server.delete(args[2]);
                    break;
                case "RECLAIM":
                    System.out.println(String.format("Changing available space to: %d KB", Integer.parseInt(args[2])));
                    server.reclaim(Integer.parseInt(args[2]));
                    break;
                case "STATE":
                    System.out.println(server.state());
                    break;
                default:
                    System.err.println("Invalid action given, unknown action:" + args[2]);
            }
        } catch(RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}

package client;

import link.RemoteInterface;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    public static void main(String[] args) {
        // check arguments
        if (args.length > 4) {
            System.err.println("Invalid number of arguments, correct usage:\njava Client <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
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
                    System.out.println("Server state:\n" + server.state());
                    break;
                default:
                    System.err.println("Invalid action given, unknown action:" + args[2]);
            }
        } catch(RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
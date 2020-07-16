package client;

import link.RemoteInterface;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// find . -name "*.java" -print | xargs javac
// find src -type f -name "*.class" -delete

// java -cp "src/" peer.PeerLauncher 1111 localhost 8000 8001 8002 8003 TLSv1.2 resources/server.keys resources/client.keys resources/truststore 123456
// java -cp "src/" peer.PeerLauncher 2222 localhost 8004 8005 8006 8007 TLSv1.2 resources/client.keys resources/server.keys resources/truststore 123456 localhost 8003
// java -cp "src/" peer.PeerLauncher 3333 localhost 8008 8009 8010 8011 TLSv1.2 resources/server.keys resources/client.keys resources/truststore 123456 localhost 8003
// java -cp "src/" peer.PeerLauncher 4444 localhost 8012 8013 8014 8015 TLSv1.2 resources/server.keys resources/client.keys resources/truststore 123456 localhost 8003
// java -cp "src/" peer.PeerLauncher 5555 localhost 8016 8017 8018 8019 TLSv1.2 resources/server.keys resources/client.keys resources/truststore 123456 localhost 8003

// java -cp "src/" client.TestApp 1111 STATE
// java -cp "src/" client.TestApp 1111 BACKUP ./src/testfiles/texto.txt 1
// java -cp "src/" client.TestApp 2222 BACKUP ./src/testfiles/me_smoking_pencil.jpg 1
// java -cp "src/" client.TestApp 1111 RESTORE ./src/testfiles/texto.txt
// java -cp "src/" client.TestApp 2222 RESTORE ./src/testfiles/me_smoking_pencil.jpg
// java -cp "src/" client.TestApp 1111 DELETE ./src/testfiles/texto.txt
// java -cp "src/" client.TestApp 2222 DELETE ./src/testfiles/me_smoking_pencil.jpg
// java -cp "src/" client.TestApp 3333 RECLAIM 0
// java -cp "src/" client.TestApp 4444 EXIT

/**
 * Class that represents the client that can communicate with a peer and test its services
 */
public class TestApp {
    /**
     * Main method.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // check arguments
        if (args.length > 4 || args.length < 2) {
            System.err.println("Invalid number of arguments, correct usage:\njava TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            System.exit(1);
        }

        String peerAP = args[0];

        try{
            Registry registry = LocateRegistry.getRegistry();
            RemoteInterface server = (RemoteInterface) registry.lookup(peerAP);

            switch (args[1]) {
                case "BACKUP":
                    if (args.length != 4) {
                        System.err.println("Invalid number of arguments for BACKUP protocol,\njava TestApp " + args[0] + " BACKUP <filepath> <desired replication degree>");
                        System.exit(2);
                    }
                    System.out.println(String.format("Requesting backup of file: %s with a replication degree of %d",
                            args[2], Integer.parseInt(args[3])));
                    server.backup(args[2], Integer.parseInt(args[3]));
                    break;
                case "RESTORE":
                    if (args.length != 3) {
                        System.err.println("Invalid number of arguments for RESTORE protocol,\njava TestApp " + args[0] + " RESTORE <original filepath>");
                        System.exit(3);
                    }
                    System.out.println(String.format("Requesting restoration of file: %s", args[2]));
                    server.restore(args[2]);
                    break;
                case "DELETE":
                    if (args.length != 3) {
                        System.err.println("Invalid number of arguments for DELETE protocol,\njava TestApp " + args[0] + " DELETE <original filepath>");
                        System.exit(4);
                    }
                    System.out.println(String.format("Requesting deletion of file: %s", args[2]));
                    server.delete(args[2]);
                    break;
                case "RECLAIM":
                    if (args.length != 3) {
                        System.err.println("Invalid number of arguments for RECLAIM protocol,\njava TestApp " + args[0] + " RESTORE <maximum storage space>");
                        System.exit(5);
                    }
                    System.out.println(String.format("Changing available space to: %d KB", Integer.parseInt(args[2])));
                    server.reclaim(Integer.parseInt(args[2]));
                    break;
                case "STATE":
                    if (args.length != 2) {
                        System.err.println("Invalid number of arguments for STATE protocol,\njava TestApp " + args[0] + " STATE");
                        System.exit(6);
                    }
                    System.out.println(server.state());
                    break;
                case "EXIT":
                    if (args.length != 2) {
                        System.err.println("Invalid number of arguments for EXIT protocol,\njava TestApp " + args[0] + " EXIT");
                        System.exit(7);
                    }
                    server.exit();
                    break;
                default:
                    System.err.println("Invalid action given, unknown action:" + args[1]);
            }
        } catch(RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}

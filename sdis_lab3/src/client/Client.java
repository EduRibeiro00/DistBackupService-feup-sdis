package client;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;

import server.RemoteInterface;

public class Client {
    public static void main(String[] args) {
        // check arguments
        if (args.length < 4) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        String hostName = args[0];
        String remoteObjectName = args[1];
        String oper = args[2];
        String dnsName = args[3];

        try{
            Registry registry = LocateRegistry.getRegistry(hostName);
            RemoteInterface server = (RemoteInterface) registry.lookup(remoteObjectName);

            // determine action
            if (oper.equals("register")) {
                if (args.length < 5) {
                    System.out.println("Invalid number of arguments");
                    System.exit(1);
                }
                String ipAddress = args[4];
                String out = server.register(dnsName, ipAddress);
                printMessage(oper, Arrays.asList(dnsName, ipAddress), out);
            } else if (oper.equals("lookup")) {
                String out = server.lookup(dnsName);
                printMessage(oper, Arrays.asList(dnsName), out);
            } else {
                System.out.println("Invalid request");
                System.exit(2);
            }

        } catch(RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
			e.printStackTrace();
		}
        
    }

    public static void printMessage(String oper, List<String> opnds, String out) {
        String output = oper;
        for (int i = 0; i < opnds.size(); i++) {
            output += (" " + opnds.get(i));
        }

        output += (" :: " + out);

        System.out.println(output);
    }
}

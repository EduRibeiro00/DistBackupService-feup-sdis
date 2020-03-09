package client;

import peer.RemoteInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String[] args) {
        // check arguments
        if (args.length < 2) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        String hostName = args[0];
        String remoteObjectName = args[1];

        try{
            Registry registry = LocateRegistry.getRegistry(hostName);
            RemoteInterface server = (RemoteInterface) registry.lookup(remoteObjectName);

            String out = server.test("ola bro");
            System.out.println(out);

        } catch(RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
}

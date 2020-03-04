package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    public static void main(String[] args) {
        // check arguments
        if (args.length != 1) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        String remoteObjectName = args[0];
        RemoteObject srvObj = new RemoteObject();

        try {
            RemoteInterface remoteObject = (RemoteInterface) UnicastRemoteObject.exportObject(srvObj, 0);
            Registry rmiReg = LocateRegistry.createRegistry(1099);
            rmiReg.bind(remoteObjectName, remoteObject);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

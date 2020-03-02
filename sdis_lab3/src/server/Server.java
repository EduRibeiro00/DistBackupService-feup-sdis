package server;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

public class Server implements RemoteInterface {
    private static HashMap<String,String> addressTable; // dns table with pairs <name, ip_address>

    public static void main(String[] args) {
        // check arguments
        if (args.length != 1) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        String remoteObjectName = args[0];



    }

    public static void printMessage(String oper, List<String> opnds, String out) {
        String output = oper;
        for (int i = 0; i < opnds.size(); i++) {
            output += (" " + opnds.get(i));
        }

        output += (" :: " + out);

        System.out.println(output);
    }


    @Override
    public String register(String dnsName, String ipAddress) throws RemoteException {
        return null;
    }


    @Override
    public String lookup(String dnsName) throws RemoteException {
        return null;
    }
}

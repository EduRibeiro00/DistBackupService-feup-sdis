package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RemoteObject implements RemoteInterface {
    private HashMap<String,String> addressTable; // dns table with pairs <name, ip_address>

    public RemoteObject() {
        addressTable = new HashMap<>();
    }

    public void printMessage(String oper, List<String> opnds, String out) {
        String output = oper;
        for (int i = 0; i < opnds.size(); i++) {
            output += (" " + opnds.get(i));
        }

        output += (" :: " + out);

        System.out.println(output);
    }


    @Override
    public String register(String dnsName, String ipAddress) {
        String response;
        if(addressTable.containsKey(dnsName)){
            response =  "Already exists";
        }
        else {
            addressTable.put(dnsName, ipAddress);
            response = String.valueOf(addressTable.size());
        }

        ArrayList<String> args = new ArrayList<>();
        args.add(dnsName);
        args.add(ipAddress);

        printMessage("register", args, response);
        return response;
    }


    @Override
    public String lookup(String dnsName) {
        String response;
        if(!addressTable.containsKey(dnsName)){
            response =  "NO_ENTRY";
        }
        else {
            String ipAddress = addressTable.get(dnsName);
            response = (dnsName + " -> " + ipAddress);
        }

        ArrayList<String> args = new ArrayList<>();
        args.add(dnsName);

        printMessage("lookup", args, response);
        return response;
    }
}

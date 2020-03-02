package client;

public class Client {
    public static void main(String[] args) {
        // check arguments
        if (args.length < 4) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        String hostName = args[0];
        String remoteObjectName = args[1];
        
    }
}

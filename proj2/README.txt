All instructions are to be executed from the project directory in a bash terminal.

Compiling and running instructions:

Compiling:
 - Inside of the root folder, run the following command: find . -name "*.java" -print | xargs javac
 - Alternatively, run any other command that is able to compile Java files.
 - If one wants to delete the classfiles, the following command can be run: find src -type f -name "*.class" -delete

Pre-running:
 - Start the RMI registry in the /src folder with the following command: rmiregistry

Starting the peers:
 - The peers should be started in seperate terminals, with the command:
      java -cp "src/" peer.PeerLauncher <REMOTE_NAME> <IP_ADDRESS> <MC_PORT> <MDB_PORT> <MDR_PORT> <CHORD_PORT> <PROTOCOL_NAME> <SERVER_KEYS> <CLIENT_KEYS> <TRUSTSTORE> <PASSWORD> (<PEER_IP> <PEER_PORT)
 - Some examples of commands to start up peers can be found in the TestApp file, and here:
      java -cp "src/" peer.PeerLauncher 1111 localhost 8000 8001 8002 8003 TLSv1.2 resources/server.keys resources/client.keys resources/truststore 123456
      java -cp "src/" peer.PeerLauncher 2222 localhost 8004 8005 8006 8007 TLSv1.2 resources/client.keys resources/server.keys resources/truststore 123456 localhost 8003
      java -cp "src/" peer.PeerLauncher 3333 localhost 8008 8009 8010 8011 TLSv1.2 resources/server.keys resources/client.keys resources/truststore 123456 localhost 8003
      java -cp "src/" peer.PeerLauncher 4444 localhost 8012 8013 8014 8015 TLSv1.2 resources/server.keys resources/client.keys resources/truststore 123456 localhost 8003

Using the client app:
 - The client app can be run using the following command:
      java -cp "src/" client.TestApp <PEER_REMOTE_NAME>
        STATE
        BACKUP <FILEPATH> <REP_DEGREE>
        RESTORE <FILEPATH>
        DELETE <FILEPATH>
        RECLAIM <NEW_STORAGE_SPACE>
        EXIT
 - Some examples of commands to run clients can be found in the TestApp file, and here:
      java -cp "src/" client.TestApp 1111 STATE
      java -cp "src/" client.TestApp 1111 BACKUP ./src/testfiles/texto.txt 1
      java -cp "src/" client.TestApp 2222 BACKUP ./src/testfiles/me_smoking_pencil.jpg 1
      java -cp "src/" client.TestApp 1111 RESTORE ./src/testfiles/texto.txt
      java -cp "src/" client.TestApp 2222 RESTORE ./src/testfiles/me_smoking_pencil.jpg
      java -cp "src/" client.TestApp 1111 DELETE ./src/testfiles/texto.txt
      java -cp "src/" client.TestApp 2222 DELETE ./src/testfiles/me_smoking_pencil.jpg
      java -cp "src/" client.TestApp 3333 RECLAIM 0
      java -cp "src/" client.TestApp 4444 EXIT

The Java SE version that was used was Java 11.0.3.

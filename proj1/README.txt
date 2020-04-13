All instructions are to be executed from the project directory in a bash terminal.

Compiling and running instructions:

Compiling:
 - Run script located in scripts/compile.sh which will create the bin directory with some sub directories necessary for the normal operation of the program.

Pre-running:
 - Start the RMI registry using the script scripts/run_rmi.sh, it will open a terminal with the RMI registry running.

Starting the peers:
 - The peer scripts will start peers in separate terminals. The title of each terminal will have the protocol version and the peer ID/access point.
 - Single peer startup:
    - Run the script scripts/run_peer.sh with the following arguments:
        <protocol_version> <peer_id> <peer_ap> <MC_addr> <MC_port> <MDB_addr> <MDB_port> <MDR_addr> <MDR_port>
        where <protocol_version>: 1.0 / 1.1
              <peer_id> / <peer_ap>: Positive integer
 - Group peer startup:
     - with the base protocol (v1.0):
        - Run the script scripts/run_base_peers.sh that will start 3 peers in separate terminals with access points 1111, 2222, 3333.
     - with the enhanced protocol (v1.1):
        - Run the script scripts/run_enhanced_peers.sh that will start 3 peers in separate terminals with access points 4444, 5555, 6666.

Using the client app:
 - The client app can be run using the script scripts/client.sh with the arguments:
    - <peer_ap> <sub_protocol> [<opnd_1> [<opnd_2]]
        where <sub_protocol>: BACKUP / RESTORE / DELETE / RECLAIM / STATE
        <opnd_1> and <opnd_2> are sub_protocol dependant:
            BACKUP <filepath> <replication_degree>
            RESTORE / DELETE <original_filepath_used_in_backup>
            RECLAIM <maximum_storage_space>
            STATE




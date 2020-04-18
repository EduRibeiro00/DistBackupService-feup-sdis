#! /usr/bin/bash

# Script for running a peer
# To be run in the root of the build tree
# No jar files used

# check number of arguments
if [ "$#" -ne 9 ]; then
  echo
  echo "Usage:"
  echo "sh $0 <protocol_version> <peer_id> <peer_ap> <MC_addr> <MC_port> <MDB_addr> <MDB_port> <MDR_addr> <MDR_port>"
  echo "<protocol_version>: 1.0 / 1.1"
  echo "<peer_id> / <peer_ap>: Positive integer"
  echo
  exit 1;
fi

# execute peer in new terminal
java peer.PeerLauncher "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8" "$9"

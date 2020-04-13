#!/bin/bash

function usage {
  echo "bash scripts/run_peer.sh <protocol_version> <peer_id> <peer_ap> <MC_addr> <MC_port> <MDB_addr> <MDB_port> <MDR_addr> <MDR_port>"
  echo "<protocol_version>: 1.0 / 1.1"
  echo "<peer_id> / <peer_ap>: Positive integer"
}

# check number of arguments
if [ "$#" -ne 9 ]; then
  echo "Usage:"
  usage
  exit 1;
fi

# execute peer in new terminal
x-terminal-emulator --tab -T "Peer $2 v$1" -e "bash -c \"cd bin; java peer.PeerLauncher $1 $2 $3 $4 $5 $6 $7 $8 $9\""

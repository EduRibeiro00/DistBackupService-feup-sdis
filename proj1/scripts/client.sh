#!/bin/bash

function usage {
  echo "sh scripts/client.sh <peer_ap> <sub_protocol> [<opnd_1> [<opnd_2]]"
  echo "<sub_protocol>: BACKUP / RESTORE / DELETE / RECLAIM / STATE"
}

# check number of arguments
if [ "$#" -eq 2 ]; then
  echo "Usage:"
  usage
  exit 1;
fi

# check sub protocol
if [ "$2" != "BACKUP" -a "$2" != "RESTORE" -a "$2" != "DELETE" -a "$2" != "RECLAIM" -a "$2" != "STATE" ]; then
  echo "Invalid sub protocol"
  echo "Usage:"
  usage
  exit 1;
fi

# check for BACKUP
if [ "$2" = "BACKUP" ]; then
  if [ "$#" -ne 4 ]; then
    echo "Invalid BACKUP call"
    echo "Usage:"
    echo "sh scripts/client.sh <peer_ap> BACKUP <file_path> <replication_degree>"
    exit 1;
  fi
fi

# check for RESTORE
if [ "$2" = "RESTORE" ]; then
  if [ "$#" -ne 3 ]; then
    echo "Invalid RESTORE call"
    echo "Usage:"
    echo "sh scripts/client.sh <peer_ap> RESTORE <file_path>"
    exit 1;
  fi
fi

# check for DELETE
if [ "$2" = "DELETE" ]; then
  if [ "$#" -ne 3 ]; then
    echo "Invalid DELETE call"
    echo "Usage:"
    echo "sh scripts/client.sh <peer_ap> DELETE <file_path>"
    exit 1;
  fi
fi

# check for RECLAIM
if [ "$2" = "RECLAIM" ]; then
  if [ "$#" -ne 3 ]; then
    echo "Invalid RECLAIM call"
    echo "Usage:"
    echo "sh scripts/client.sh <peer_ap> RECLAIM <max_space>"
    exit 1;
  fi
fi

# execute client in another terminal
cd bin && java client.TestApp $1 $2 $3 $4
wait
#! /usr/bin/bash

# Script for running the test app
# To be run at the root of the compiled tree
# No jar files used

# check number of arguments
if [ "$#" -lt 2 ]; then
  echo
  echo "Usage:"
  echo "sh $0 <peer_ap> <sub_protocol> [<opnd_1> [<opnd_2]]"
  echo "<sub_protocol>: BACKUP / RESTORE / DELETE / RECLAIM / STATE"
  echo
  exit 1;
fi

# check sub protocol
if [ "$2" != "BACKUP" -a "$2" != "RESTORE" -a "$2" != "DELETE" -a "$2" != "RECLAIM" -a "$2" != "STATE" ]; then
  echo
  echo "Invalid sub protocol"
  echo "Usage:"
  echo "sh $0 <peer_ap> <sub_protocol> [<opnd_1> [<opnd_2]]"
  echo "<sub_protocol>: BACKUP / RESTORE / DELETE / RECLAIM / STATE"
  echo
  exit 1;
fi

# check for BACKUP
if [ "$2" = "BACKUP" ]; then
  if [ "$#" -ne 4 ]; then
    echo
    echo "Invalid BACKUP call"
    echo "Usage:"
    echo "sh $0 <peer_ap> BACKUP <file_path> <replication_degree>"
    echo
    exit 1;
  fi
fi

# check for RESTORE
if [ "$2" = "RESTORE" ]; then
  if [ "$#" -ne 3 ]; then
    echo
    echo "Invalid RESTORE call"
    echo "Usage:"
    echo "sh $0 <peer_ap> RESTORE <file_path>"
    echo
    exit 1;
  fi
fi

# check for DELETE
if [ "$2" = "DELETE" ]; then
  if [ "$#" -ne 3 ]; then
    echo
    echo "Invalid DELETE call"
    echo "Usage:"
    echo "sh $0 <peer_ap> DELETE <file_path>"
    echo
    exit 1;
  fi
fi

# check for RECLAIM
if [ "$2" = "RECLAIM" ]; then
  if [ "$#" -ne 3 ]; then
    echo
    echo "Invalid RECLAIM call"
    echo "Usage:"
    echo "sh $0 <peer_ap> RECLAIM <max_space>"
    echo
    exit 1;
  fi
fi

# execute client in current terminal session
java client.TestApp $1 $2 $3 $4
wait
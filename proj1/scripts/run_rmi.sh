#!/bin/bash

echo "Lanching rmi registry..."
x-terminal-emulator --tab -T "Rmi Registry" -e 'bash  -c "cd bin; echo \"Rmi registry running...\"; rmiregistry"'
sleep 1


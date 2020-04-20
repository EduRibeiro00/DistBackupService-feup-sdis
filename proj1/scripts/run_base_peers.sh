#!/bin/bash

echo "Launching base peers..."

x-terminal-emulator --tab -T "Peer 1111 v1.0" -e 'bash -c "cd bin; java peer.PeerLauncher 1.0 1111 1111 225.0.0.1 8080 225.0.0.1 8081 225.0.0.1 8082"'
x-terminal-emulator --tab -T "Peer 2222 v1.0" -e 'bash -c "cd bin; java peer.PeerLauncher 1.0 2222 2222 225.0.0.1 8080 225.0.0.1 8081 225.0.0.1 8082"'
x-terminal-emulator --tab -T "Peer 3333 v1.0" -e 'bash -c "cd bin; java peer.PeerLauncher 1.0 3333 3333 225.0.0.1 8080 225.0.0.1 8081 225.0.0.1 8082"'

echo
wait

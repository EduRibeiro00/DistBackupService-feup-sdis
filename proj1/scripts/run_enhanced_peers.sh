#!/bin/bash

echo "Launching enhanced peers..."

x-terminal-emulator --tab -T "Peer 4444 v1.1" -e 'bash -c "cd bin; java peer.PeerLauncher 1.1 4444 4444 225.0.0.1 8080 225.0.0.1 8081 225.0.0.1 8082"'
x-terminal-emulator --tab -T "Peer 5555 v1.1" -e 'bash -c "cd bin; java peer.PeerLauncher 1.1 5555 5555 225.0.0.1 8080 225.0.0.1 8081 225.0.0.1 8082"'
x-terminal-emulator --tab -T "Peer 6666 v1.1" -e 'bash -c "cd bin; java peer.PeerLauncher 1.1 6666 6666 225.0.0.1 8080 225.0.0.1 8081 225.0.0.1 8082"'

echo
wait
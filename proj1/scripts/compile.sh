#!/bin/bash

rm -rf bin
mkdir -p bin

echo "Compiling service..."
echo
javac $(find src | grep .java) -d bin 2> /dev/null &

mkdir -p bin/peer/chunks
mkdir -p bin/peer/files

echo "Compiled successfuly"
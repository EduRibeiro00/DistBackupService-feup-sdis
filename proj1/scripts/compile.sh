#! /usr/bin/bash

# Compilation script
# To be executed in the root of the package (source code) hierarchy
# Compiled code is placed under ./build/

rm -rf build
mkdir -p build

javac $(find . | grep .java) -d build 2> /dev/null &

mkdir -p build/peer/chunks
mkdir -p build/peer/files
#! /usr/bin/bash

# Clean-up script
# To be executed in the root of the build tree
# Requires at most one argument: the peer id
# Cleans the directory tree for storing
#  both the chunks and the restored files of
#  a single peer


# Check number input arguments
argc=$#

if [ $argc -eq 1 ]
then
	peer_id=$1
else
  echo
	echo "Usage:"
	echo "sh $0 [<peer_id>]]"
	echo
	exit 1
fi

rm -rf peer/chunks/"$peer_id"
rm -rf peer/files/"$peer_id"


# Clean the directory tree for storing files
# For a crash course on shell commands check for example:
# Command line basi commands from GitLab Docs':	https://docs.gitlab.com/ee/gitlab-basics/command-line-commands.html
# For shell scripting try out the following tutorials of the Linux Documentation Project
# Bash Guide for Beginners: https://tldp.org/LDP/Bash-Beginners-Guide/html/index.html
# Advanced Bash Scripting: https://tldp.org/LDP/abs/html/


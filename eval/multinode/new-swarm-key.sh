#!/bin/sh
# Writes the swarm key of the experiment network next to this script. Only nodes
# with this file can join the network, and the key is separate from the key of
# docker/kubo, so the experiment nodes never meet the nodes of the tutorial.
# The file stays out of version control (see .gitignore).
set -e
cd "$(dirname "$0")"
if [ -e swarm.key ]; then
  echo "swarm.key already exists; delete it first to start a new network" >&2
  exit 1
fi
printf '/key/swarm/psk/1.0.0/\n/base16/\n%s\n' \
  "$(od -vN 32 -An -tx1 /dev/urandom | tr -d ' \n')" > swarm.key
chmod 600 swarm.key
echo "Wrote $(pwd)/swarm.key"

#!/bin/sh
# Writes a new swarm key for the private IPFS network next to this script.
# Every node that should join the network needs a copy of the same file, and
# no node without it can connect. Keep the file out of version control.
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

#!/bin/sh
# Sets the topology of the experiment network and restarts the nodes, because
# Kubo reads Peering.Peers only at start-up.
#
#   sh topology.sh peered   every node keeps a connection to every other node
#   sh topology.sh dht      mn-a, mn-b and mn-c keep a connection only to the hub
#                           mn-h, so a fetcher finds the publisher through the DHT
#
# The script prints the peers of every node after the restart.
set -e
here="$(cd "$(dirname "$0")" && pwd)"
compose="docker compose -f $here/compose.yaml"
topology="${1:?usage: topology.sh peered|dht}"
nodes="ecorefs-mn-a ecorefs-mn-b ecorefs-mn-c ecorefs-mn-h"

peer_id() {
  docker exec "$1" ipfs id -f '<id>'
}

# Prints the nodes that a node should peer with in the chosen topology.
peers_of() {
  node="$1"
  case "$topology" in
    peered)
      for other in $nodes; do
        [ "$other" != "$node" ] && echo "$other"
      done
      ;;
    dht)
      if [ "$node" = "ecorefs-mn-h" ]; then
        echo "ecorefs-mn-a ecorefs-mn-b ecorefs-mn-c" | tr ' ' '\n'
      else
        echo "ecorefs-mn-h"
      fi
      ;;
    *)
      echo "unknown topology: $topology" >&2
      exit 2
      ;;
  esac
}

for node in $nodes; do
  entries=""
  for other in $(peers_of "$node"); do
    entries="$entries{\"ID\":\"$(peer_id "$other")\",\"Addrs\":[\"/dns4/$other/tcp/4001\"]},"
  done
  docker exec "$node" ipfs config --json Peering.Peers "[${entries%,}]"
done

$compose restart > /dev/null 2>&1
for attempt in $(seq 60); do
  healthy=$(docker ps --filter name=ecorefs-mn- --filter health=healthy --format '{{.Names}}' | wc -l)
  [ "$healthy" -ge 4 ] && break
  sleep 2
done
if [ "$healthy" -lt 4 ]; then
  echo "only $healthy of 4 nodes are healthy after the restart" >&2
  exit 1
fi
# Peering dials the listed peers within a few seconds of the start
sleep 8
for node in $nodes; do
  count=$(docker exec "$node" ipfs swarm peers | wc -l)
  echo "$node ($topology) has $count peers"
done

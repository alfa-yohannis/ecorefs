#!/bin/sh
# Makes every running node of the private network keep a connection to every
# other node, then restarts them. Kubo's Peering list needs the peer IDs, which
# exist only after a node's first start, so run this once after the first
# `docker compose up`, and again after adding a node to compose.yaml.
set -e
compose_file="$(dirname "$0")/compose.yaml"
nodes=$(docker compose -f "$compose_file" ps --format '{{.Name}}')

for node in $nodes; do
  peers=""
  for other in $nodes; do
    if [ "$other" != "$node" ]; then
      id=$(docker exec "$other" ipfs id -f '<id>')
      peers="$peers{\"ID\":\"$id\",\"Addrs\":[\"/dns4/$other/tcp/4001\"]},"
    fi
  done
  docker exec "$node" ipfs config --json Peering.Peers "[${peers%,}]"
  echo "$node peers with: $(echo "$nodes" | grep -v "^$node\$" | tr '\n' ' ')"
done

docker compose -f "$compose_file" restart

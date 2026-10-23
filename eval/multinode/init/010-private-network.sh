#!/bin/sh
# Keeps an experiment node inside the private network on every start. The script
# follows docker/kubo/init/010-private-network.sh with one change: mDNS is off,
# because the topology of the experiment comes only from Peering.Peers, which
# topology.sh sets. A node that found its peers through mDNS would connect to
# every other node and spoil the topology without a direct connection.
set -e

install -m 0400 /run/ecorefs/swarm.key "$IPFS_PATH/swarm.key"

ipfs bootstrap rm --all > /dev/null
ipfs config --json AutoConf.Enabled false
ipfs config --json DNS.Resolvers '{}'
ipfs config --json Ipns.DelegatedPublishers '[]'
ipfs config --json Routing.DelegatedRouters '[]'
# compose.yaml makes the publisher and the hub DHT servers and the fetchers DHT
# clients, so the records live on the servers and a fetcher has to ask for them
ipfs config Routing.Type "${ECOREFS_ROUTING_TYPE:-dhtserver}"
ipfs config --json Discovery.MDNS.Enabled false
ipfs config --json AutoTLS.Enabled false
ipfs config --json Swarm.RelayClient.Enabled false
ipfs config --json Swarm.RelayService.Enabled false
# Private networks work over TCP only
ipfs config --json Addresses.Swarm '["/ip4/0.0.0.0/tcp/4001"]'
ipfs config --json Swarm.Transports.Network.Websocket false
ipfs config --json Swarm.Transports.Network.QUIC false
ipfs config --json Swarm.Transports.Network.WebTransport false
ipfs config --json Swarm.Transports.Network.WebRTCDirect false

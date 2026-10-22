#!/bin/sh
# Keeps the node inside the private network on every start: it installs the
# shared swarm key, drops the public bootstrap peers, and switches off the
# internet services that Kubo's "auto" settings would contact. The nodes find
# each other on the Docker network through mDNS.
set -e

install -m 0400 /run/ecorefs/swarm.key "$IPFS_PATH/swarm.key"

ipfs bootstrap rm --all > /dev/null
ipfs config --json AutoConf.Enabled false
ipfs config --json DNS.Resolvers '{}'
ipfs config --json Ipns.DelegatedPublishers '[]'
ipfs config --json Routing.DelegatedRouters '[]'
# Every node serves the DHT, which carries the IPNS records between the nodes
ipfs config Routing.Type dhtserver
ipfs config --json Discovery.MDNS.Enabled true
ipfs config --json AutoTLS.Enabled false
ipfs config --json Swarm.RelayClient.Enabled false
ipfs config --json Swarm.RelayService.Enabled false
# Private networks work over TCP only
ipfs config --json Addresses.Swarm '["/ip4/0.0.0.0/tcp/4001"]'
ipfs config --json Swarm.Transports.Network.Websocket false
ipfs config --json Swarm.Transports.Network.QUIC false
ipfs config --json Swarm.Transports.Network.WebTransport false
ipfs config --json Swarm.Transports.Network.WebRTCDirect false

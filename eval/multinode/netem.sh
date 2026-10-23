#!/bin/sh
# Adds a network delay between the experiment nodes with tc netem, or removes it.
#
#   sh netem.sh none     no added delay
#   sh netem.sh lan      1 ms one-way delay, 0.2 ms jitter
#   sh netem.sh wan50    50 ms one-way delay, 10 ms jitter
#   sh netem.sh wan100   100 ms one-way delay, 20 ms jitter
#
# Every node delays the packets that it sends on the swarm port 4001, in both
# directions of a connection (destination port 4001 when the node dialled,
# source port 4001 when the node accepted). A round trip between two nodes
# therefore takes twice the one-way delay. The API port 5001 stays undelayed,
# because the harness on the host stands for an application next to its own
# node, not for a remote client. The Kubo image has no tc, so a helper image
# runs tc inside the network namespace of each node.
#
# netem draws the delay of each packet on its own, so with jitter a packet can
# overtake the one before it. TCP takes such reordering for loss, and a 40 MB
# fetch over wan100 took 23 s once and 65 s the next time. With a rate, netem
# sends no packet before the previous one, which keeps the order; the same
# fetch then took 17 to 18 s. The rate of 1 Gbit/s is above what a fetch
# reaches without delay, so it does not slow the fetches down.
set -e
profile="${1:?usage: netem.sh none|lan|wan50|wan100}"
nodes="ecorefs-mn-a ecorefs-mn-b ecorefs-mn-c ecorefs-mn-h"
image="ecorefs-netem:1"

case "$profile" in
  none) delay="" ;;
  lan) delay="delay 1ms 0.2ms distribution normal rate 1gbit" ;;
  wan50) delay="delay 50ms 10ms distribution normal rate 1gbit" ;;
  wan100) delay="delay 100ms 20ms distribution normal rate 1gbit" ;;
  *) echo "unknown profile: $profile" >&2; exit 2 ;;
esac

if ! docker image inspect "$image" > /dev/null 2>&1; then
  docker build -q -t "$image" "$(dirname "$0")/netem" > /dev/null
fi

tc_in() {
  node="$1"
  shift
  docker run --rm --net "container:$node" --cap-add NET_ADMIN "$image" "$@"
}

for node in $nodes; do
  # Start from the default queueing, ignoring the error when no root qdisc is set
  tc_in "$node" qdisc del dev eth0 root 2> /dev/null || true
  if [ -n "$delay" ]; then
    tc_in "$node" qdisc add dev eth0 root handle 1: prio bands 3 priomap 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1
    # shellcheck disable=SC2086
    # A queue of 10,000 packets holds more than a delayed fetch ever has in flight, so netem drops nothing
    tc_in "$node" qdisc add dev eth0 parent 1:3 handle 30: netem limit 10000 $delay
    tc_in "$node" filter add dev eth0 parent 1: protocol ip prio 1 u32 match ip dport 4001 0xffff flowid 1:3
    tc_in "$node" filter add dev eth0 parent 1: protocol ip prio 2 u32 match ip sport 4001 0xffff flowid 1:3
  fi
  echo "$node: $(tc_in "$node" qdisc show dev eth0 | grep -o 'netem.*' || echo 'no delay')"
done

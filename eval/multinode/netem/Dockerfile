# Helper image that holds tc. The Kubo image has no tc, so netem.sh runs this
# image inside the network namespace of each node (--net container:<node>) with
# the NET_ADMIN capability, and the image changes the queueing of that node.
FROM alpine:3.20
RUN apk add --no-cache iproute2 iproute2-tc
ENTRYPOINT ["tc"]

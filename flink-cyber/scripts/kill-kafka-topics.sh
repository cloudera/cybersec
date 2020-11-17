#!/bin/sh

BOOTSTRAP="$(hostname):9092"

for topic in $(kafka-topics --bootstrap-server $BOOTSTRAP --list | grep -v __)
do
  kafka-topics --bootstrap-server $BOOTSTRAP --delete --topic $topic
done
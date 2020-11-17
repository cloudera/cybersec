#!/bin/sh

SCHEMAS=$(curl -ssl http://$(hostname):7788/api/v1/schemaregistry/schemas | jq ".entities[].schemaMetadata.name" -r)

for SCHEMA in $SCHEMAS
do
  curl -ssl -X DELETE "http://$(hostname):7788/api/v1/schemaregistry/schemas/$SCHEMA"
done

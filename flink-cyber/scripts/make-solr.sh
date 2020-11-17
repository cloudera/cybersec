#!/bin/sh

pushd solr

for a in *;
do
  solrctl config --upload $a $a
  solrctl collection --create $a -c $a -s 1
done

popd

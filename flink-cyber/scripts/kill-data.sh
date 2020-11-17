#!/bin/sh

hdfs dfs -rm -skipTrash -f -r /data/original/*
hdfs dfs -rm -skipTrash -f -r /data/logs/*

#!/bin/sh

flink run --jobmanager yarn-cluster -yjm 1024 -ytm 1024 --detached --parallelism 2 --yarnname "Caracal Generator" caracal-generator-0.0.1-SNAPSHOT.jar caracal-generator.properties
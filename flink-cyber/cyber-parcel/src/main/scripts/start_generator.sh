#!/bin/sh
### Test GENERATOR
flink run --jobmanager yarn-cluster -yjm 1024 -ytm 1024 --detached --yarnname "Caracal Generator" jar/caracal-generator-0.0.1-SNAPSHOT.jar conf/generator.properties

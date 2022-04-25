#!/bin/bash
CMD=$1

export CYBERSEC_BIN=${CYBERSEC_BIN:-$CYBERSEC_BIN}
export SCRIPTS_DIR=${SCRIPTS_DIR:-${CONF_DIR}/scripts}
export CYBERSEC_CONF_DIR=${CONF_DIR}/cybersec-conf

source  ${SCRIPTS_DIR}/set-dependencies.sh

case $CMD in
  (start_generator)
    echo "Starting generator job"
    export LOGGER_JARS=${CDH_PARCEL_HOME}/lib/hadoop/client/log4j.jar:${CDH_PARCEL_HOME}/lib/hadoop/client/slf4j-api.jar

    exec ${CDH_FLINK_BIN}/flink run --jobmanager yarn-cluster -yjm 1024 -ytm 1024 --detached --yarnname "Caracal Generator" ${CYBERSEC_BIN}/caracal-generator-0.0.1-SNAPSHOT.jar ${CYBERSEC_CONF_DIR}/generator.properties
    ;;
  (client)
    echo "Client EHO"
    set-dependencies
    ;;
  (*)
    echo "Don't understand [$CMD]"
    ;;
esac

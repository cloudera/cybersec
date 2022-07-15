#!/bin/bash
CMD=$1

export CYBERSEC_BIN=${CYBERSEC_BIN:-$CYBERSEC_BIN}
export SCRIPTS_DIR=${SCRIPTS_DIR:-${CONF_DIR}/scripts}
export CYBERSEC_CONF_DIR=${CONF_DIR}/cybersec-conf


case $CMD in
  (client)
    echo "Client echo"
    ;;
  (*)
    echo "Don't understand [$CMD]"
    ;;
esac

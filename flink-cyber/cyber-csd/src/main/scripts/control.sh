#!/bin/bash
CMD=$1

export CYBERSEC_BIN=${CYBERSEC_BIN:-$CYBERSEC_BIN}
export SCRIPTS_DIR=${SCRIPTS_DIR:-${CONF_DIR}/scripts}
export CYBERSEC_CONF_DIR=${CONF_DIR}/cybersec-conf


case $CMD in
  (start-parser-ui)
    echo "Client echo"
    exec ${CYBERSEC_BIN}/start-parser-ui start-foreground
    ;;
  (*)
    echo "Don't understand [$CMD]"
    ;;
esac

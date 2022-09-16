#!/bin/bash
CMD=$1

export CYBERSEC_BIN=${CYBERSEC_BIN:-$CDH_CYBERSEC_BIN}
export SCRIPTS_DIR=${SCRIPTS_DIR:-${CONF_DIR}/scripts}
export CYBERSEC_CONF_DIR=${CONF_DIR}/cybersec-conf


case $CMD in
  (start-parser-ui)
    echo "ParserUI echo $CYBERSEC_BIN/start-parser-ui  ${CYBERSEC_BIN}/start-parser-ui"
    exec ${CYBERSEC_BIN}/start-parser-ui start
    ;;
  (*)
    echo "Don't understand [$CMD]"
    ;;
esac

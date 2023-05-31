#!/bin/bash
#
# Copyright 2020 - 2022 Cloudera. All Rights Reserved.
#
# This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
# except in compliance with the License. You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0.
#
# This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
# either express or implied. Refer to the License for the specific permissions and
# limitations governing your use of the file.
#

CMD=$1

export CYBERSEC_BIN=${CYBERSEC_BIN:-$CDH_CYBERSEC_BIN}
export SCRIPTS_DIR=${SCRIPTS_DIR:-${CONF_DIR}/scripts}
export CYBERSEC_CONF_DIR=${CONF_DIR}/cybersec-conf


case $CMD in
  (start-parser-ui)
    echo "ParserUI echo $CYBERSEC_BIN/start-parser-ui"
    get_generic_java_opts
    exec ${CYBERSEC_BIN}/start-parser-ui start
    ;;
  (*)
    echo "Don't understand [$CMD]"
    ;;
esac

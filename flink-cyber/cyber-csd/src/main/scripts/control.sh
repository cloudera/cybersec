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
set -x

DEFAULT_CYBERSEC_HOME=/usr/lib/cyberserc
export CYBERSEC_HOME=${CYBERSEC_HOME:-$CDH_CYBERSEC_HOME}
export CYBERSEC_HOME=${CYBERSEC_HOME:-$DEFAULT_CYBERSEC_HOME}
export CYBERSEC_BIN=${CYBERSEC_BIN:-$CDH_CYBERSEC_BIN}
export SCRIPTS_DIR=${SCRIPTS_DIR:-${CONF_DIR}/scripts}
export CYBERSEC_CONF_DIR=${CONF_DIR}/cybersec-conf

CMD=$1

. $COMMON_SCRIPT

function log {
  timestamp=$(date)
  echo "$timestamp: $1"       #stdout
  echo "$timestamp: $1" 1>&2; #stderr
}

log "CYBERSEC_HOME: $CYBERSEC_HOME"
log "CONF_DIR: $CONF_DIR"
log "CMD: $CMD"

# If Ranger service is selected as dependency, add Ranger plugin specific parameters
if [[ -n "${RANGER_SERVICE}" && "${RANGER_SERVICE}" != "none" ]]; then

  # Populate the required field for 'ranger-cybersec-security.xml'
  RANGER_CYBERSEC_PLUGIN_SSL_FILE="${CONF_DIR}/ranger-cybersec-policymgr-ssl.xml"
  perl -pi -e "s#\{\{RANGER_CYBERSEC_PLUGIN_SSL_FILE}}#${RANGER_CYBERSEC_PLUGIN_SSL_FILE}#g" "${CONF_DIR}"/ranger-cybersec-security.xml

  # set +x
  # Populate the required fields for 'ranger-cybersec-policymgr-ssl.xml'. Disable printing
  # the commands as they are sensitive fields.
  if [ -n "${CYBERSEC_TRUSTSTORE_LOCATION}" ] && [ -n "${CYBERSEC_TRUSTORE_PASSWORD}" ]; then
    perl -pi -e "s#\{\{RANGER_PLUGIN_TRUSTSTORE}}#${CYBERSEC_TRUSTSTORE_LOCATION}#g" "${CONF_DIR}"/ranger-cybersec-policymgr-ssl.xml
    if [[ "${RANGER_KEYSTORE_TYPE}" = "bcfks" ]]
    then
      STORETYPE="bcfks"
    else
      STORETYPE="jceks"
    fi
    RANGER_PLUGIN_TRUSTSTORE_CRED_FILE="${STORETYPE}://file${CONF_DIR}/rangerpluginssl.${STORETYPE}"

    # Use jars from Ranger admin package to generate the trsustore credential file.
    RANGER_ADMIN_CRED_LIB="${PARCELS_ROOT}/${PARCEL_DIRNAMES}/lib/ranger-admin/cred/lib/"
    export JAVA_HOME=${JAVA_HOME};"${JAVA_HOME}"/bin/java ${CSD_JAVA_OPTS} -cp "${RANGER_ADMIN_CRED_LIB}/*" org.apache.ranger.credentialapi.buildks create sslTrustStore -value "${CYBERSEC_TRUSTORE_PASSWORD}" -provider "${RANGER_PLUGIN_TRUSTSTORE_CRED_FILE}"
    perl -pi -e "s#\{\{RANGER_PLUGIN_TRUSTSTORE_CRED_FILE}}#${RANGER_PLUGIN_TRUSTSTORE_CRED_FILE}#g" "${CONF_DIR}"/ranger-cybersec-policymgr-ssl.xml
  else
    perl -pi -e "s#\{\{RANGER_PLUGIN_TRUSTSTORE}}##g" ${CONF_DIR}/ranger-cybersec-policymgr-ssl.xml
    perl -pi -e "s#\{\{RANGER_PLUGIN_TRUSTSTORE_CRED_FILE}}##g" ${CONF_DIR}/ranger-cybersec-policymgr-ssl.xml
  fi
  set -x

  # Populate the required fields for 'ranger-cybersec-audit.xml'
  KEYTAB_FILE="${CONF_DIR}/cybersec.keytab"
  perl -pi -e "s#\{\{KEYTAB_FILE}}#${KEYTAB_FILE}#g" ${CONF_DIR}/ranger-cybersec-audit.xml

  cp -f ${CONF_DIR}/hadoop-conf/core-site.xml ${CONF_DIR}/

  # Collect Ranger repo users. Not sure if this is necessary or not
  cybersec_repo_users="${cybersec_service_user_name}"
  if [[ ${cybersec_service_user_name} != ${cybersec_principal_name} && -n ${cybersec_principal_name} ]]; then
    cybersec_repo_users+=",${cybersec_principal_name}"
  fi
  export cybersec_repo_users

  # Optionally create the Cybersec service in Ranger.
  "${CONF_DIR}"/scripts/ranger_init.sh -c create -s "${RANGER_CYBERSEC_SERVICE_NAME}"

  . "${CONF_DIR}/scripts/vars.sh"
  perl -pi -e "s#\Q${RANGER_CYBERSEC_SERVICE_NAME}\E#${SANITIZED_RANGER_SERVICE_NAME}#g" ${CONF_DIR}/ranger-cybersec-security.xml
fi

case $CMD in
  (start-parser-ui)
    echo "ParserUI echo $CYBERSEC_BIN/cs-start-parser-ui"
    get_generic_java_opts
    exec ${CYBERSEC_BIN}/cs-start-parser-ui start
    ;;
  (*)
    echo "Don't understand [$CMD]"
    ;;
esac
set +x

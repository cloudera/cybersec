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

function log {
  echo "$(date): $*"
}

# Prints args to stderr and exits with failure
function die() {
    echo "$@" 1>&2
    exit 1
}

function check_dependencies() {
  # is ranger service existing?
  if [[ -z "${RANGER_SERVICE}" || "${RANGER_SERVICE}" == "none" ]]
  then
      # no Ranger, nothing to do
      log "No Ranger service defined, nothing to do"
      exit 0
  fi

  # get kerberos ticket
  if [[ ${ENABLE_SECURITY} == "true" ]]
  then
    kinit -k -t "${CONF_DIR}/cybersec.keytab" "${cybersec_principal}"
    log "kerberos tickets" $(klist)
  else
      log "Kerberos not enabled."
  fi
}

function check_env_variables() {

  for variable_name in "$@"
  do
    if [[ -v ${variable_name} ]]
    then
      log "${variable_name}: ${!variable_name}"
    else
      die "${variable_name} was not defined"
    fi
  done
}

function write_env_variables() {

  SCRIPT_NAME="${CONF_DIR}/scripts/vars.sh"
  echo "#!/usr/bin/env bash" > "${SCRIPT_NAME}"

  for variable_name in "$@"
  do
    if [[ -v ${variable_name} ]]
    then
      printf "export %s=%s\n" "${variable_name}" "${!variable_name}" >> "${SCRIPT_NAME}"
    fi
  done

}

function check_sanitize_service_name() {

  # if service name is the magic string
  if [[ "${service}" == "{{GENERATED_RANGER_SERVICE_NAME}}" ]]
  then
    # sensible default: CM generated name
    service=${RANGER_REPO_NAME}
  fi

  # sanitize the repo name
  service=${service//@([^[:alnum:]_-])/_}

  # assign to variable for futher use and save
  export SANITIZED_RANGER_SERVICE_NAME=${service}
}

function wait_ranger() {
  local ranger_url=$1

  counter=1
  counter_max=5

  until curl -k --output /dev/null --silent --head "${ranger_url}" || (( counter > counter_max ))
  do
    echo "waiting to connect to ${ranger_url} ..."
    sleep 5
    (( counter++ ))
  done

  if (( counter > counter_max ))
  then
    echo "Ranger is unreachable at $ranger_url"
    return 1
  fi

  return 0
}

function ranger_service_rest() {

  API_URL_PATH=${1}
  HTTP_METHOD=${2}
  INPUT=${3:+--data @${3}}

  # handling multiple Ranger servers, it is possible to get a comma-separated list of URLs
  local ranger_url="null"
  local default_separator="$IFS"
  IFS=,
  read -ra RANGER_URLS <<<"$RANGER_REST_URL"
  for url in "${RANGER_URLS[@]}"; do
    wait_ranger $url
    if (( $? == 0 )); then
       ranger_url="$url"
       break
    fi
  done
  IFS="$default_separator"

  if [ "$ranger_url" = "null" ]; then
     die "Ranger is unreachable."
  fi

  ranger_api_url="${ranger_url%/}/${API_URL_PATH#/}"
  read -r http_code < <(curl --output /dev/null -s -k -w "%{http_code}" --negotiate -u :  -X "${HTTP_METHOD}" -H "Content-Type:application/json" ${INPUT} "${ranger_api_url}")

  log "result" "${HTTP_METHOD} ${API_URL_PATH}" "${http_code}"
}

# for debugging
set -x
shopt -s extglob

log "Host: $(hostname)"
log "Pwd: $(pwd)"

check_dependencies
check_env_variables RANGER_REST_URL CLUSTER_NAME RANGER_REPO_NAME

repo_json="repo_create.json"
servicedef_json="ranger-servicedef-cybersec.json"

# Process command line arguments
while getopts "c:s:r:" arg
do
  case $arg in
    c)
      command=${OPTARG}
      ;;
    s)
      service=${OPTARG}
      ;;
    r)
      repo_json=${OPTARG}
      ;;
    *)
      command=create
  esac
done

# SANITIZED_RANGER_SERVICE_NAME contains sanitized service name after this call
check_sanitize_service_name
write_env_variables SANITIZED_RANGER_SERVICE_NAME

case $command in

  (create)
    # test for existence
    ranger_service_rest "service/public/v2/api/service/name/${SANITIZED_RANGER_SERVICE_NAME}" GET

    # create, if does not exist
    if [[ "${http_code}" == "404" ]]
    then
      ranger_service_rest "service/plugins/definitions" POST <(envsubst < "$CONF_DIR/aux/$servicedef_json")
      ranger_service_rest "service/public/v2/api/service" POST <(envsubst < "$CONF_DIR/aux/$repo_json")

      if [[ "${http_code}" == "200" ]]
      then
        exit 0;
      else
        die "API call failed for creating ranger repo: ${SANITIZED_RANGER_SERVICE_NAME}"
      fi
	elif [[ "${http_code}" == "200" ]]; then
	  log "Repo already exists: ${SANITIZED_RANGER_SERVICE_NAME}"
    else
      die "API call failed, repo: ${SANITIZED_RANGER_SERVICE_NAME}"
    fi

    ;;

  (get)
    ranger_service_rest "service/public/v2/api/service/name/${SANITIZED_RANGER_SERVICE_NAME}" GET
    if [[ "${http_code}" == "200" ]]
    then
      exit 0;
    else
      die "API call failed for getting ranger repo: ${SANITIZED_RANGER_SERVICE_NAME}"
    fi
    ;;

  (delete)
    ranger_service_rest "service/public/v2/api/service/name/${SANITIZED_RANGER_SERVICE_NAME}" DELETE
    if [[ "${http_code}" == "204" ]]
    then
      exit 0;
    else
      die "API call failed for deleting ranger repo: ${SANITIZED_RANGER_SERVICE_NAME}"
    fi
    ;;

  (*)
    die "Don't understand [$command]"
    ;;

esac
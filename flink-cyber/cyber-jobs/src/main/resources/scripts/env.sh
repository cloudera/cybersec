#!/usr/bin/env bash

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

# Determine the location of the script to locate parcel
# Reference: http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SOURCE="${BASH_SOURCE[0]}"
BIN_DIR="$( dirname "$SOURCE" )"
while [ -h "$SOURCE" ]
do
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
  BIN_DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd )"
done
BIN_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

ETC_DIR=$BIN_DIR/../etc
TEMPLATES_DIR="$ETC_DIR"/conf/templates
OPT_DIR="/opt/cloudera/parcels"
CYBERSEC_OPT_DIR=${CYBERSEC_OPT_DIR:-"$OPT_DIR/CYBERSEC"}

# Autodetect JAVA_HOME if not defined
if [ -e /usr/bin/bigtop-detect-javahome ] ; then
  . /usr/bin/bigtop-detect-javahome
fi


if [[ -d "$JAVA_HOME" ]]; then
    JAVA_RUN="$JAVA_HOME"/bin/java
    KEYTOOL_RUN="$JAVA_HOME"/bin/keytool
else
    JAVA_RUN=java
    KEYTOOL_RUN=keytool
fi

# Detect if /etc/cybersec/conf has been introduced, otherwise fall back to the parcel local option.
# /etc/cybersec/conf is to be populated by the Flink Gateway role, but it is not not mandatory.
DEFAULT_CYBERSEC_CONF_DIR=$ETC_DIR

if [ -d /etc/cybersec/conf ] ; then
  DEFAULT_CYBERSEC_CONF_DIR=/etc/cybersec/conf
fi

# Verify that the hadoop command exists. This is expected as the Flink parcel declares dependency
# on the CDH parcel.
if ! [ -x "$(command -v hadoop)" ]; then
  echo '[ERROR] The hadoop command is not installed. Verify your Cloudera Distribution for Hadoop installation.' >&2
  exit 1
fi

# Verify that the CDH parcel directory exists. This is expected as the Flink parcel declares dependency
# on the CDH parcel.
CDH_PARCEL_HOME=$BIN_DIR/../../CDH
if ! [ -d $CDH_PARCEL_HOME ]; then
  echo '[ERROR] The CDH parcel directory was not found. Verify your Cloudera Distribution for Hadoop installation.' >&2
  exit 1
fi

function run_java_class() {
  if [ "$#" -lt 2 ]; then
      echo "Usage: $0 jar_prefix class_name {options}" >&2
      exit 1
  fi

  local jar_prefix=$1
  local class_name=$2
  local options=()
  local jar_path
  local flink_dist
  local log4j_config
  local flink_dist

  for arg in "$@:3"
  do
    options+=("$arg")
  done

  jar_path=$(cs-lookup-jar "$jar_prefix")
  flink_dist=$(cs-lookup-jar "flink_dist")
  log4j_config=$(cs-lookup-jar "log4j.properties")
  lib_jars=$(find "$CYBERSEC_OPT_DIR/lib/" -name "*.jar" | tr '\n' ':' | sed 's/:$/\n/')
  flink_dist=$(find "$OPT_DIR/FLINK/" -name "flink-dist*.jar")

  "$JAVA_RUN" -Dlog4j.configuration="$log4j_config" -Dlog4j.configurationFile="$log4j_config" -cp "$jar_path:$lib_jars:$flink_dist" "$class_name" "${options[@]}"
}

# read_properties_into_variables <property_file_name>
# Read the name value pairs
#   split at first equals to key and value
#   convert property name to legal shell variable name
#   set shell variable to property variable
function read_properties_into_variables() {
  while read -r line; do
    [[ "$line" =~ ^([[:space:]]*|[[:space:]]*#.*)$ ]] && continue
    value=${line#*=}
    key=${line%"=$value"}
    key=$(echo $key | tr '.' '_')
    eval ${key}=${value}
  done <$1
}

#  <file name> <property_name>
# get the value of a property from a file
function get_property_value() {
  cat "$1" | grep -v "^.*#" | grep "$2" | cut -d '=' -f 2-
}

function init_key_store() {
  if [ -f "kerberos.properties" ]; then
      generated_dir=generated
      mkdir -p "$generated_dir"
      keystore_file=flink_internal.jks
      keystore_name=$generated_dir/$keystore_file
      if [ ! -f "$keystore_name" ]; then
         ssl_properties=$generated_dir/internal_ssl.properties
         keystore_pass=$(openssl rand -base64 18)
         ${KEYTOOL_RUN} -genkeypair -alias flink.internal -keystore "${keystore_name}" -dname "CN=flink.internal" -storepass "${keystore_pass}" -keyalg RSA -keysize 4096 -storetype PKCS12
         printf 'flink.internal.keystore=%s\nflink.internal.password=%s\n' "${keystore_file}" "${keystore_pass}" > ${ssl_properties}
         chmod 600 ${ssl_properties}
         chmod 600 ${keystore_name}
      fi
  fi
}

function get_kerberos_config() {
  local security_options=()
  kerberos_properties="kerberos.properties"
  internal_ssl_properties="generated/internal_ssl.properties"

  if [ -f "${kerberos_properties}" ]; then
    read_properties_into_variables "${kerberos_properties}"
    read_properties_into_variables "${internal_ssl_properties}"
    security_options+=("-yD" "security.kerberos.login.keytab=${kerberos_keytab}")
    security_options+=("-yD" "security.kerberos.login.principal=${kerberos_principal}")
    security_options+=("-yD" "security.ssl.internal.enabled=true")
    security_options+=("-yD" "security.ssl.internal.keystore=${flink_internal_keystore}")
    security_options+=("-yD" "security.ssl.internal.key-password=${flink_internal_password}")
    security_options+=("-yD" "security.ssl.internal.keystore-password=${flink_internal_password}")
    security_options+=("-yD" "security.ssl.internal.truststore=${flink_internal_keystore}")
    security_options+=("-yD" "security.ssl.internal.truststore-password=${flink_internal_password}")
    security_options+=("-yt" "generated/${flink_internal_keystore}")
  fi
  eval "$1"=\('${security_options[@]}'\)
}


ship_config() {
  local temp_arr
  if [ -f "$2" ]; then
    echo "INFO: HBase Configuration: adding file $2"
    temp_arr+=("$1 $2")
  fi
  eval "$3"=\('${arr_temp[@]}'\)
}

override_hbase() {
  local arr_temp=()
  ship_config "-yt" "core-site.xml" arr_temp
  ship_config "-yt" "hdfs-site.xml" arr_temp
  ship_config "-yt" "hbase-site.xml" arr_temp

  if [ "${#arr_temp[@]}" -eq 0 ]; then
      if [ -f "hbase-conf/hbase-site.xml" ]; then
          hbase_conf_dir="$(pwd)/hbase-conf"
          echo "INFO: HBase Configuration: using directory $hbase_conf_dir"
          export "HBASE_CONF_DIR=$hbase_conf_dir"
      else
          echo "INFO: HBase Configuration: using default"
      fi
  fi

  eval "$1"=\('${arr_temp[@]}'\)
}

# Set environment variables
export HADOOP_HOME=${HADOOP_HOME:-/usr/lib/hadoop}
export HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-/etc/hadoop/conf}
export CYBERSEC_OPT_DIR
export CYBERSEC_HOME=${CYBERSEC_HOME:-/var/lib/cybersec}
export CYBERSEC_LOG_DIR=${CYBERSEC_LOG_DIR:-$CYBERSEC_HOME/log}
export CYBERSEC_CONF_DIR=${CYBERSEC_CONF_DIR:-$DEFAULT_CYBERSEC_CONF_DIR}
export HADOOP_CLASSPATH=${HADOOP_CLASSPATH:-$(hadoop classpath)}
export HBASE_CONF_DIR=${HBASE_CONF_DIR:-/etc/hbase/conf}
export JAVA_RUN
export KEYTOOL_RUN
export TEMPLATES_DIR

CYBERSEC_DIRNAME=${PARCEL_DIRNAME:-"CYBERSEC"}
CDH_CYBERSEC_HOME="$PARCELS_ROOT"/"$CYBERSEC_DIRNAME"
CDH_CYBERSEC_BIN="$PARCELS_ROOT"/"$CYBERSEC_DIRNAME"/bin
export CDH_CYBERSEC_HOME
export CDH_CYBERSEC_BIN
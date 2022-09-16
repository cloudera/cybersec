#!/usr/bin/env bash

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

# Autodetect JAVA_HOME if not defined
if [ -e /usr/bin/bigtop-detect-javahome ] ; then
  . /usr/bin/bigtop-detect-javahome
fi


if [[ -d "$JAVA_HOME" ]]; then
    JAVA_RUN="$JAVA_HOME"/bin/java
else
    JAVA_RUN=java
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

# Set environment variables
export HADOOP_HOME=${HADOOP_HOME:-/usr/lib/hadoop}
export HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-/etc/hadoop/conf}
export CYBERSEC_OPT_DIR=${CYBERSEC_OPT_DIR:-/opt/cloudera/parcels/CYBERSEC}
export CYBERSEC_HOME=${CYBERSEC_HOME:-/var/lib/cybersec}
export CYBERSEC_LOG_DIR=${CYBERSEC_LOG_DIR:-$CYBERSEC_HOME/log}
export CYBERSEC_CONF_DIR=${CYBERSEC_CONF_DIR:-$DEFAULT_CYBERSEC_CONF_DIR}
export HADOOP_CLASSPATH=${HADOOP_CLASSPATH:-$(hadoop classpath)}
export HBASE_CONF_DIR=${HBASE_CONF_DIR:-/etc/hbase/conf}
export JAVA_RUN
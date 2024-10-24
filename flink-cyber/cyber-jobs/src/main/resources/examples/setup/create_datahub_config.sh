#!/usr/bin/env bash

report_usage_error() {
   echo "$(basename $0) $1" >&2
   exit 2
}

report_fail() {
   echo "$(basename $0) ERROR: $1" >&2
   exit 2
}

report_info() {
  echo "INFO: $1" >&2
}

if [[ $# -ne 2 ]]; then
  report_usage_error "<environment_name> <properties_file>"
fi

get_dh_name() {
  cdp datahub list-clusters | jq -r '.clusters[] | select(.clusterName | contains ("'"$1"'")) | select(.workloadType | contains ("'$2'")) | .clusterName '
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

env_name=$1
properties_file=$2

if [[ -f "$properties_file" ]]; then
  read_properties_into_variables "$properties_file"
else
    report_fail "Properties file $properties_file can't be read.  Check path and permissions."
fi

echo "When prompted, enter your workload user password."

found_env=$(cdp environments list-environments | jq -r '.environments[] | select(.environmentName=="'"${env_name}"'") | .environmentName')
if [[ -z "${found_env}" ]]; then
    report_fail "Environment ${env_name} does not exist."
fi

config_dir=../pipelines
workload_user=$(cdp iam get-user | jq -r '.user.workloadUsername')

# discover hive configs
if [[ ! -z "${hive_datahub_name}" ]]; then
    hive_dh=$(get_dh_name "${hive_datahub_name}" "Hive")
    if [[ ! -z "$hive_dh" ]]; then
        hive_zip=$config_dir/hive-conf.zip
        hive_conf="$config_dir/hive-conf"
        hive_cm_api=$(cdp datahub describe-cluster --cluster-name "$hive_dh" | jq -r '.cluster.endpoints.endpoints[] | select (.serviceName | contains("CM-API")) | .serviceUrl')
        report_info "resetting hive configs from datahub ${hive_dh}"
        rm -f "$hive_zip"
        rm -rf "$hive_conf"
        curl -S -s -o "${hive_zip}" -u "${workload_user}" ${hive_cm_api}/v41/clusters/${hive_dh}/services/hive_on_tez/clientConfig
        if [[ -f "$hive_zip" ]]; then
           tar -zxvf "$hive_zip" -C "$config_dir"
           rm -f "$hive_conf/core-site.xml"
           rm -f "$hive_conf/yarn-site.xml"
        else
            report_fail "Could not get hive configuration."
        fi
    else
      report_fail "Hive datahub '${hive_datahub_name}' not found in environment '${env_name}'"
    fi
else
  report_info "Hive is not configured.  Property hive_datahub_name not defined in properties file"
fi



# discover kafka connection config
if [[ ! -z "${kafka_datahub_name}" ]]; then
    kafka_dh_name=$(get_dh_name "${kafka_datahub_name}" "Kafka")
    if [[ -z "${kafka_dh_name}" ]]; then
        report_fail "Environment '${env_name}' does not contain a datahub named '${kafka_datahub_name}' containing Kafka"
    fi
else
    report_fail "Kafka is not configured.  Property kafka_datahub_name not defined in properties file"
fi
schema_registry=$(cdp datahub describe-cluster --cluster-name ${kafka_dh_name} | jq -r '.cluster.instanceGroups[] | select(.name | contains("master")) | .instances[].fqdn')
kafka_broker=$(cdp datahub describe-cluster --cluster-name  ${kafka_dh_name} | jq -r '.cluster.endpoints.endpoints[] | select (.serviceName | contains("KAFKA_BROKER")) | .serviceUrl' | sed 's/ //g')

# opdb (hbase and phoenix) connection config
if [[ ! -z "${opdb_database_name}" ]]; then
    opdb_cluster_name=$(cdp opdb list-databases --environment-name ${env_name} | jq -r '.databases[] | select(.databaseName | contains ("'"${opdb_database_name}"'")) | .databaseName')
    phoenix_query_server_host=NO_OPDB_CLUSTER
    if [[ ! -z "$opdb_cluster_name" ]]; then
        report_info "Resetting OPDB configs from datahub ${opdb_cluster_name}"
        opdb_client_url=$(cdp opdb describe-client-connectivity --environment-name ${env_name} --database-name ${opdb_cluster_name} | jq -r '.connectors[] | select(.name=="hbase") | .configuration.clientConfigurationDetails[].url')
        hbase_zip="$config_dir/hbase-config.zip"
        hbase_conf="$config_dir/hbase-conf"
        rm -f "$hbase_zip"
        rm -rf "$hbase_conf"
        curl -S -s -f -o "$hbase_zip" -u "${workload_user}" "${opdb_client_url}"
        if [[ -f "$hbase_zip" ]]; then
           tar -zxvf "$hbase_zip" -C "$config_dir"
        else
            report_fail "Could not get HBase configuration."
        fi
        base_opdb_services_url=$(echo ${opdb_client_url} | sed -e 's/hbase\/clientConfig//')
        report_info "getting Phoenix connection settings"
        phoenix_query_server_host=$(curl -S -s -u ${workload_user} ${base_opdb_services_url}/phoenix/roles | jq -r '.items[] | select (.type | contains("PHOENIX_QUERY_SERVER")) | .hostRef.hostname')
    else
        report_fail "OPDB database ${hive_datahub_name} not found in environment ${env_name}"
    fi
else
    report_info "HBase and Phoenix are not configured.  Property opdb_database_name not defined in properties file" >&2
fi 

cdp environments get-keytab --environment-name $env_name | jq -r '.contents' | base64 --decode > ${config_dir}/krb5.keytab

cdp environments get-root-certificate --environment-name $env_name | jq -r '.contents' > ${config_dir}/environment_cert.crt


env_truststore_pass=$(openssl rand -base64 18)
env_truststore_dir=${config_dir}/configs
mkdir -p ${env_truststore_dir}
env_truststore_file=${env_truststore_dir}/environment-truststore.jks
rm -f "${env_truststore_file}"

keytool -import  -trustcacerts -keystore "${env_truststore_file}" -alias trust_ca -file ${config_dir}/environment_cert.crt -noprompt -storepass ${env_truststore_pass}
cybersec_user_princ=`ktutil --keytab=${config_dir}/krb5.keytab list | awk '(NR>3) {print $3}' | uniq`

hostname=$(hostname -f)
for TEMPLATE in templates/*; do filename=$(basename $TEMPLATE); cat $TEMPLATE | sed -e 's,ENV_TRUSTSTORE_PW,'"$env_truststore_pass"',g' -e 's/KAFKA_BROKER/'"${kafka_broker}"'/g' -e 's/KERBEROS_PRINCIPAL/'"$cybersec_user_princ"'/g' -e 's/SCHEMA_REGISTRY/'"$schema_registry"'/g' -e 's/PHOENIX_QUERY_SERVER/'"$phoenix_query_server_host"'/g' > ${config_dir}/$filename ; done

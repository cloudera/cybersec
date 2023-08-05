if [[ $# -ne 2 ]]; then
    echo "$(basename $0) <environment_name> <cluster_prefix>" >&2
    exit 2
fi

set -x

env_name=$1
cluster_prefix=$2
config_dir=../pipelines
workload_user=$(cdp iam get-user | jq -r '.user.workloadUsername')

# discover kafka connection config 
kafka_cluster_name=$(cdp datahub list-clusters | jq -r '.clusters[] | select(.clusterName | contains ("'"${cluster_prefix}"'")) | select(.workloadType | contains ("Kafka")) | .clusterName ')
schema_registry=$(cdp datahub describe-cluster --cluster-name ${kafka_cluster_name} | jq -r '.cluster.instanceGroups[] | select(.name | contains("master")) | .instances[].fqdn')
kafka_broker=$(cdp datahub describe-cluster --cluster-name  ${kafka_cluster_name} | jq -r '.cluster.endpoints.endpoints[] | select (.serviceName | contains("KAFKA_BROKER")) | .serviceUrl' | sed 's/ //g')

# opdb (hbase and phoenix) connection config
opdb_cluster_name=$(cdp opdb list-databases --environment-name ${env_name} | jq -r '.databases[] | select(.databaseName | contains ("'"${cluster_prefix}"'")) | .databaseName') 
if [[ ! -z "$opdb_cluster_name" ]]; then
    opdb_client_url=$(cdp opdb describe-client-connectivity --environment-name se-sandboxx-aws --database-name msm-cod-1 | jq -r '.connectors[] | select(.name=="hbase") | .configuration.clientConfigurationDetails[].url')
    hbase_zip="$config_dir/hbase-config.zip"
    curl -f -o "$hbase_zip" -u "cduby" "$opdb_client_url"
    if [[ -f "$hbase_zip" ]]; then 
       unzip "$hbase_zip" -d "$config_dir"
    else 
        echo "ERROR: could not get hbase configuration."
        exit 2
    fi
fi 

cdp environments get-keytab --environment-name $env_name | jq -r '.contents' | base64 --decode > ${config_dir}/krb5.keytab

cdp environments get-root-certificate --environment-name $env_name | jq -r '.contents' > ${config_dir}/environment_cert.crt

rm ${config_dir}/environment-truststore.jks

env_truststore_pass=$(openssl rand -base64 18)
env_truststore_file=environment_truststore.jks

keytool -import  -trustcacerts -keystore ${config_dir}/environment-truststore.jks -alias trust_ca -file ${config_dir}/environment_cert.crt -noprompt -storepass ${env_truststore_pass} 
cybersec_user_princ=`ktutil --keytab=${config_dir}/krb5.keytab list | awk '(NR>3) {print $3}' | uniq`
echo "PRINCIPAL="$cybersec_user_princ

hostname=$(hostname -f)
for TEMPLATE in templates/*; do filename=$(basename $TEMPLATE); cat $TEMPLATE | sed -e 's/ENV_TRUSTSTORE_PW/'"$env_truststore_pass"'/g' -e 's/KAFKA_BROKER/'"${kafka_broker}"'/g' -e 's/KERBEROS_PRINCIPAL/'"$cybersec_user_princ"'/g' -e 's/SCHEMA_REGISTRY/'"$schema_registry"'/g' > ${config_dir}/$filename ; done

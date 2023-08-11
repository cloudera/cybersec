set -x
user=$(whoami)
beeline_conf="beeline.properties"
connection_options=()
if [[ -f "${beeline_conf}" ]]; then
    jdbc_url=$(cat ${beeline_conf} | grep jdbc | cut -d= -f2-) 
    connection_options=("-u" "${jdbc_url};user=${user}")
fi

echo "creating hive events table"
beeline -p -f ../setup/hive_table.sql "${connection_options[@]}"

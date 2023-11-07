hbase_conf_opt=()
if [ -f "hbase-conf/hbase-site.xml" ]; then
    hbase_conf_opt+=("--config" "hbase-conf")
fi


source ../setup/env_hbase.sh

echo "create enrichment hbase table"
echo "create 'example_enrichments', 'cf'" | hbase "${hbase_conf_opt[@]}" shell -n


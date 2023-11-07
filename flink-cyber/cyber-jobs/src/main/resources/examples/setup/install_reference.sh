source ref_dir.sh

echo "copy enrichment reference data to hdfs:$ref_data_dir"
hdfs dfs -mkdir -p $ref_data_dir/basic
hdfs dfs -put -f ../pipelines/basic/triage/samples/*.csv $ref_data_dir/basic
hdfs dfs -mkdir -p $ref_data_dir/full
hdfs dfs -put -f ../pipelines/full/triage/samples/*.csv $ref_data_dir/full


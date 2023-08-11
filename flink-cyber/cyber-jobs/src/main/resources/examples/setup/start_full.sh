source ref_dir.sh

cybersec_user=$(whoami)
branch_name=examples
pipe_name=full

./setup_triage_geo.sh ${pipe_name}

cd ../pipelines/

../setup/create_hive_tables.sh
../setup/create_hbase.sh

echo "starting generator"
cs-restart-generator ${branch_name} ${pipe_name} squid

echo "starting parser"
cs-restart-parser ${branch_name} ${pipe_name} main

echo "loading enrichments"
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/majestic_million.csv --pipe ${pipe_name} --branch ${branch_name} --enrich majestic_million
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/malicious_domain.csv --pipe ${pipe_name} --branch ${branch_name} --enrich malicious_domain
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/domain_category.csv --pipe ${pipe_name} --branch ${branch_name} --enrich domain_category
cs-publish-samples --pipe $pipe_name --stage triage --file threatq.json --topic examples.$pipe_name.threatq.input

echo "loading triage scoring rules"
cs-upsert-rule --branch ${branch_name} --pipe ${pipe_name} --rule dga_rule.json 

echo "start triaging"
cs-restart-triage ${branch_name} ${pipe_name}

cs-upsert-rule --branch ${branch_name} --pipe ${pipe_name} --profile main --rule first_seen_rule.json

echo "start indexing"
cs-restart-index ${branch_name} ${pipe_name}

echo "start profiling"
cs-restart-profile ${branch_name} ${pipe_name} main


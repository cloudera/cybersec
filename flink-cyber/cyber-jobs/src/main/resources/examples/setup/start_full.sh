source ref_dir.sh

cybersec_user=$(whoami)
branch_name=examples
pipe_name=full

./setup_triage_geo.sh ${pipe_name}

cd ../pipelines/
mkdir -p logs/${pipe_name}

../setup/create_hbase.sh

echo "starting generator"
cs-restart-generator ${branch_name} ${pipe_name} squid  >logs/${pipe_name}/start_generator.log 2>&1

echo "starting parser"
cs-restart-parser ${branch_name} ${pipe_name} main >logs/${pipe_name}/start_parser.log 2>&1

echo "loading enrichments"
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/majestic_million.csv --pipe ${pipe_name} --branch ${branch_name} --enrich majestic_million >logs/${pipe_name}/load_majestic_million.log 2>&1
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/malicious_domain.csv --pipe ${pipe_name} --branch ${branch_name} --enrich malicious_domain >logs/${pipe_name}/load_malicious_domain.log 2>&1
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/domain_category.csv --pipe ${pipe_name} --branch ${branch_name} --enrich domain_category >logs/${pipe_name}/load_domain_category.log 2>&1
cs-publish-samples --pipe $pipe_name --stage triage --file threatq.json --topic examples.$pipe_name.threatq.input >logs/${pipe_name}/publish_threatq.log 2>&1

echo "loading triage scoring rules"
cs-upsert-rule --branch ${branch_name} --pipe ${pipe_name} --rule dga_rule.json >logs/${pipe_name}/upsert_rule_1.log 2>&1

echo "start triaging"
cs-restart-triage ${branch_name} ${pipe_name} >logs/${pipe_name}/start_triage.log 2>&1

cs-upsert-rule --branch ${branch_name} --pipe ${pipe_name} --profile main --rule first_seen_rule.json >logs/${pipe_name}/upsert_rule_2.log 2>&1

echo "start indexing"
cs-restart-index ${branch_name} ${pipe_name} >logs/${pipe_name}/start_index.log 2>&1

echo "start profiling"
cs-restart-profile ${branch_name} ${pipe_name} main >logs/${pipe_name}/start_profile.log 2>&1


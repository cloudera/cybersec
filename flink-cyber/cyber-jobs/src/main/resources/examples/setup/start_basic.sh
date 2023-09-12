source ref_dir.sh

cybersec_user=$(whoami)
branch_name=examples
pipe_name=basic

./setup_triage_geo.sh basic

cd ../pipelines/

mkdir -p logs/basic
echo "starting generator"
cs-restart-generator ${branch_name} ${pipe_name} squid >logs/basic/start_generator.log 2>&1

echo "starting parser"
cs-restart-parser ${branch_name} ${pipe_name} main >logs/basic/start_parser.log 2>&1

echo "loading enrichments"
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/majestic_million.csv --pipe ${pipe_name} --branch ${branch_name} --enrich majestic_million >logs/basic/load_majestic_million.log 2>&1
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/malicious_domain.csv --pipe ${pipe_name} --branch ${branch_name} --enrich malicious_domain >logs/basic/load_malicious_domain.log 2>&1

echo "loading triage scoring rules"
cs-upsert-rule --branch ${branch_name} --pipe ${pipe_name} --rule dga_rule.json  >logs/basic/upsert_rule_1.log 2>&1

echo "start triaging"
cs-restart-triage ${branch_name} ${pipe_name} >logs/basic/start_triage.log 2>&1

cs-upsert-rule --branch ${branch_name} --pipe ${pipe_name} --profile main --rule anomalous_bytes.json >logs/basic/upsert_rule_2.log 2>&1
cs-restart-profile ${branch_name} ${pipe_name} main >logs/basic/start_profile.log 2>&1

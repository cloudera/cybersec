set -x 
source ref_dir.sh

cybersec_user=$(whoami)
branch_name=examples
pipe_name=basic

./setup_triage_geo.sh basic

cd ../pipelines/

echo "starting generator"
cs-restart-generator ${branch_name} ${pipe_name} squid

echo "starting parser"
cs-restart-parser ${branch_name} ${pipe_name} main

echo "loading enrichments"
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/majestic_million.csv --pipe ${pipe_name} --branch ${branch_name} --enrich majestic_million
cs-load-enrich --file hdfs:${ref_data_dir}/${pipe_name}/malicious_domain.csv --pipe ${pipe_name} --branch ${branch_name} --enrich malicious_domain

echo "loading triage scoring rules"
cs-upsert-rule --branch ${branch_name} --pipe ${pipe_name} --rule dga_rule.json 

echo "start triaging"
cs-restart-triage ${branch_name} ${pipe_name}


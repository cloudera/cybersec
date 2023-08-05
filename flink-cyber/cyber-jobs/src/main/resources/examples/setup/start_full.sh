cybersec_user=$(whoami)
branch_name=examples
pipe_name=basic

echo "starting generator"
cs-restart-generator ${pipe_name} ${branch_name} squid

echo "starting parser"
cs-restart-parser ${pipe_name} ${branch_name}

echo "loading enrichments"
cs-load-enrich --file hdfs:/user/${cybersec_user}/cybersec/reference/enrich/majestic_million.csv --pipe ${pipe_name} --branch ${branch_name} --enrich majestic_million
cs-load-enrich --file hdfs:/user/${cybersec_user}/cybersec/reference/enrich/malicious_domain.csv --pipe ${pipe_name} --branch ${branch_name} --enrich malicious_domain

echo "loading triage scoring rules"
cs-upsert-rule --branch ${branch_name} --pipe ${pipe_name} --rule dga_rule.json 

echo "start triaging"
cs-restart-triage ${branch_name} ${pipe_name}


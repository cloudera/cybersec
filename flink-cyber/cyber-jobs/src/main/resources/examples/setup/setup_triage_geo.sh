source geo_dir.sh

pipe=$1

echo "checking geo enrichments mmdbs"
echo "city enabled = ${geo_city_enabled}"
echo "asn enabled = ${geo_asn_enabled}"

triage_template="../pipelines/${pipe}/triage/triage.properties.template"
triage_template_bak="${triage_template}.bak"
if [ ! -f "$triage_template_bak" ]; then
    cp ${triage_template} ${triage_template_bak}
fi

cat ${triage_template_bak} | sed -e s,GEO_CITY_ENABLED,${geo_city_enabled},g -e s,GEO_ASN_ENABLED,${geo_asn_enabled},g -e s,GEO_CITY_MMDB,${geo_city_file},g -e s,GEO_ASN_MMDB,${geo_asn_file},g > ${triage_template}


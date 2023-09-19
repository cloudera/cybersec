cybersec_user=$(whoami)
geo_data_dir=/user/$cybersec_user/cybersec/example/reference/geo

geo_city_file=${geo_data_dir}/GeoLite2-City.mmdb
geo_asn_file=${geo_data_dir}/GeoLite2-ASN.mmdb

hdfs dfs -test -f "$geo_city_file"
if [ "$?" -ne 0 ]; then
   geo_city_enabled="false"
else
   geo_city_enabled="true"
fi

hdfs dfs -test -f "$geo_asn_file"
if [ "$?" -ne 0 ]; then
   geo_asn_enabled="false"
else
   geo_asn_enabled="true"
fi


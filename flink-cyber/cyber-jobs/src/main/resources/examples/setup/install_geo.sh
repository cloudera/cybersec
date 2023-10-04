source geo_dir.sh
hdfs dfs -mkdir -p $geo_data_dir

echo "extracting maxmind enrichment mmdbs to hdfs:$geo_data_dir"
for MMDB_FILE in  GeoLite2-*.gz; do  tar -zxvf "${MMDB_FILE}"; done

geo_city=$(ls -1 GeoLite*/GeoLite2-City.mmdb)
geo_asn=$(ls -1 GeoLite*/GeoLite2-ASN.mmdb)
if [ -f "$geo_city" ]; then
    hdfs dfs -put -f GeoLite*/GeoLite2-City.mmdb $geo_data_dir
else
    echo "WARN: Could not find Geo City DB.  Geocode enrichment will not be enabled.  Download the GeoCity database from maxmind.com."
fi
if [ -f "$geo_asn" ]; then 
   hdfs dfs -put -f GeoLite*/GeoLite2-ASN.mmdb $geo_data_dir
else 
   echo "WARN: Could not find Geo ASN DB.  Asn enrichment will not be enabled.  Download the GeoCity database from maxmind.com."
fi


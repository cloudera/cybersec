# Retention Cleaner

Uses a config file to determine the retention of long-term data stored on HDFS and Hive locations. It is NOT responsible for TTL in Enrichments (which should be handled by HBase record TTL), Flink State rotations (handled by streaming jobs) or Kafka topic retention (handled by Kafka).

This should be run as a cron script on a master node, and should not be considered a critical service. It should be entirely idempotent and safe to run multiple times.

The script will find and remove files and entries from tables which are older than the specified time delta for the data source and location.

Use java.security properties to specify ssl truststores for the cluster, and JaaS to specify kerberos parameters.
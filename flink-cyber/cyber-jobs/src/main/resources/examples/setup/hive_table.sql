CREATE DATABASE IF NOT EXISTS cyber;
use cyber;
CREATE TABLE IF NOT EXISTS `events`(
   `id` string,
   `ts` timestamp,
   `message` string,
   `fields` map<string,string>,
   `ip_src_addr` string,
   `ip_dst_addr` string,
   `ip_src_port` string,
   `ip_dst_port` string,
   `source` string,
   `ip_dst_addr_geo_latitude` double,
   `ip_dst_addr_geo_longitude` double,
   `originalsource_topic` string,
   `originalsource_partition` int,
   `originalsource_offset` bigint,
   `originalsource_signature` binary,
   `ip_dst_addr_geo_country` string,
   `ip_dst_addr_geo_city` string,
   `ip_dst_addr_geo_state` string,
   `ip_proto` string,
   `dns_query` string,
   `domain` string,
   `squid_request_status` string,
   `ip_src_addr_asn_asn_org` string,
   `ip_src_addr_asn_asn_mask` string,
   `ip_src_addr_asn_asn` string,
   `http_method` string,
   `content_type` string,
   `transfer_size` bigint,
   `peer_code` string,
   `http_status_code` int,
   `ip_dst_addr_asn_asn_org` string,
   `ip_dst_addr_asn_asn_mask` string,
   `ip_dst_addr_asn_asn` string,
   `client_identity` string,
   `response_time` bigint,
   `dga_model_legit` boolean,
   `cyberscore` float,
   `cyberscore_details` array<struct<`ruleid`:string, `score`:float, `reason`:string>>)
 PARTITIONED BY (
   `dt` string,
   `hr` string)
 CLUSTERED BY (
  source)
 INTO 2 BUCKETS
 stored as orc tblproperties("transactional"="true");

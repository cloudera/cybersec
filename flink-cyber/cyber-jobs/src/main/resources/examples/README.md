# Quickstart Example Pipelines

This directory contains example pipelines to demonstrate the capabilities of the cybersec toolkit and help you get started quickly. 

## Basic Pipeline
The basic pipeline operates on a minimal cluster and performs the pipeline:
1. Parses squid logs.
2. Enriches squid logs with Maxmind Geo location and ASN, malicious domain threat intelligence, majestic million rank and mock DGA domain classification model service results.
3. Scores each event by combining the scores from the dga rule and malicious_domain rule.
4. Applies counting and aggregating profiles.

## Full Pipeline
The full pipeline operates on a complete cluster and performs the pipeline:
1. Parses squid logs.
2. Enriches with domain category and stellar extension language enrichments in addition to basic pipeline enrichments.
3. Scores same as basic pipeline.
4. Indexes events to a Hive table.
5. Applies first seen profile in addition to the basic pipeline profiles.
6. Writes profile measurements to Phoenix.
7. Writes first seen times to HBase.

## Getting Started
### Cluster Software Requirements
Provision a cluster or clusters that contain the services required by the pipeline.  

#### CDP Base Cluster
Install the following services on the CDP Base Cluster.

| Service |    Basic | Full        |
|---------| ---------| ------------|
| Flink   |    X     |  X          |
| Yarn    |    X     |  X          |
| Kafka   |    X     |  X          |
| Cloudera Schema Registry | X | X |
| Ranger   |    X     | X |
| Kerberos |    X     | X |
| TLS      |    X     | X |
| SQL Stream Builder    |    recommended | recommended |
| Streams Messaging Manager | recommended | recommended |
| Hbase    |          | X |
| Phoenix  |          | X |
| Hive on Tez     |          | X |

#### CDP Public Cloud

Provision the resources below in the same CDP Environment and use the same prefix at the begining of each resource name:

| Resource Type | Configuration | Basic | Full |
| -----------------| ------| ---- | ------|
| Data Hub | Streaming Analytics - Light Duty | X  | X |
| Data Hub | Streams Messaging - Light Duty | X | X |
| Data Hub | Data Engineering |   | X |
| Operational Database | Micro with Cloud Storage| | X |

## Installation
### Option 1 - For demo or production: Install Cybersec parcel using Cloudera Manager
1. If you have admin permissions and sudo access on the cluster containing Flink, install the cybersec parcel to the local Cloudera parcel repo or a network parcel repo.
2. Add the cybersec service to the cluster where Flink is installed.

### Option 2 - For demo: Add cybersec parcel to the command line path.
1. If you don't have admin permissions or sudo access on the cluster containing Flink, scp the cyber-parcel/target/CYBERSEC-*.parcel to a Flink gateway node.
2. Ssh to the Flink gateway node.
3. Extract the parcel using tar.  This will create a directory with the same name as the .parcel file:
```shell script
[cduby@cduby-csa-081423-master0 ~]$ tar xvf CYBERSEC-2.3.1-1.16.1-csadh1.10.0.0-cdh7.2.17.0-334-2308141830-el7.parcel 
CYBERSEC-2.3.1-1.16.1-csadh1.10.0.0-cdh7.2.17.0-334-2308141830/meta/
...
```
4. Create a link from CYBERSEC to the parcel directory.
```shell script
[cduby@cduby-csa-081423-master0 ~]$ ln -s CYBERSEC-2.3.1-1.16.1-csadh1.10.0.0-cdh7.2.17.0-334-2308141830 CYBERSEC
[cduby@cduby-csa-081423-master0 ~]$ ls -ld CYBERSEC
lrwxrwxrwx. 1 cduby cduby 62 Aug 14 20:41 CYBERSEC -> CYBERSEC-2.3.1-1.16.1-csadh1.10.0.0-cdh7.2.17.0-334-2308141830
[cduby@cduby-csa-081423-master0 ~]$ ls CYBERSEC
bin  etc  jobs  lib  meta  tools
```
5. Edit the shell configuration where the PATH is defined.  For example, edit .bash_profile.  
```shell script
## The shell config file, maybe different.  Locate the definition of PATH in your configs.
[cduby@cduby-csa-081423-master0 ~]$ vi .bash_profile
```
6. Add $HOME/CYBERSEC/bin to the PATH
```shell script
### this is an example, use your path here
PATH=$PATH:$HOME/CYBERSEC/bin:$HOME/.local/bin:$HOME/bin
export PATH
```
7. Source the shell config or log out and log back in again to refresh the shell settings.  Check the availability of the cybersec commands in the path.
```shell script
[cduby@cduby-csa-081423-master0 ~]$ source .bash_profile
[cduby@cduby-csa-081423-master0 ~]$ which cs-restart-parser
~/CYBERSEC/bin/cs-restart-parser
```

## Configuration

### Add Ranger policies required by each pipeline
1. Open the Ranger UI and add permissions required by the pipeline.

### Prepare configuration files

#### CDP Base
1. Copy the files in examples/setup/templates to example/pipelines

```
cd cybersec/flink-cyber/
```
2. Edit the .properties files in example/pipelines with the correct settings for the cluster.
2. If the Hbase service is not in the same cluster as Flink, download the Hbase client configs from Cloudera Manager.  Move the hbase config zip to the pipelines directory.  Unzip the hbase configuration files.     
3. If the Hive service is not in the same cluster as Flink, download the Hive on tez client configs from Cloudera Manager.  Move the hive config zip to the pipelines directory.  Unzip the hive config files.
4. If using a separate Hive cluster, remove the hive_conf/core-site.xml and hive-conf/yarn-site.xml files.  

#### CDP Public Cloud
1. If necessary, install the [CDP CLI client](https://docs.cloudera.com/cdp-public-cloud/cloud/cli/topics/mc-cli-client-setup.html).
2. Run the command line ./create_datahub_config.sh <environment_name> <prefix>. When prompted enter your workload password.
```shell script
cduby@cduby-MBP16-21649 examples % cd cybersec/flink-cyber/cyber-jobs/src/main/resources/examples/setup 
cduby@cduby-MBP16-21649 setup % ./create_datahub_config.sh se-sandboxx-aws cduby
cleaning up hive configs
Enter host password for user 'cduby':
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 11102    0 11102    0     0   2870      0 --:--:--  0:00:03 --:--:--  2876
x hive-conf/mapred-site.xml
x hive-conf/hdfs-site.xml
x hive-conf/hive-site.xml
x hive-conf/atlas-application.properties
x hive-conf/log4j.properties
x hive-conf/hadoop-env.sh
x hive-conf/log4j2.properties
x hive-conf/redaction-rules.json
x hive-conf/core-site.xml
x hive-conf/yarn-site.xml
x hive-conf/hive-env.sh
x hive-conf/beeline-site.xml
Enter host password for user 'cduby':
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  5828  100  5828    0     0   5394      0  0:00:01  0:00:01 --:--:--  5436
x hbase-conf/hdfs-site.xml
x hbase-conf/atlas-application.properties
x hbase-conf/hbase-omid-client-config.yml
x hbase-conf/hbase-env.sh
x hbase-conf/core-site.xml
x hbase-conf/log4j.properties
x hbase-conf/hbase-site.xml
x hbase-conf/jaas.conf
Certificate was added to keystore
PRINCIPAL=cduby@SE-SANDB.A465-9Q4K.CLOUDERA.SITE
```
### (Optional) Download Maxmind Database
1. Optionally download the binary (mmdb) version of the [Maxmind GeoLite2 City and ASN Databases](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data). Create a Maxmind account login if you don't have one already.  If you don't download the databases, the triaging job will operate but events will not have geocode (country, city, lat, lon) or asn (network) enrichments. 
2. cp the GeoLite2 .tar.gz files to the examples/setup directory.
```shell script
cduby@cduby-MBP16-21649 templates % cp GeoLite2-*.tar.gz examples/setup
```
3. Scp the examples directory tree to the flink gateway host.
```shell script
scp -r examples <user>@<flink_gateway_host>:/home/<user>
```

## Starting the pipeline
We are ready to start!

1. Ssh to the flink_gateway_host.
2. Run the setup.sh script
```shell script
[cduby@cduby-csa-081423-master0 ~]$ cd examples/setup/
[cduby@cduby-csa-081423-master0 setup]$ ./setup.sh 
extracting maxmind enrichment mmdbs to hdfs:/user/cduby/cybersec/example/reference/geo
GeoLite2-ASN_20210216/
GeoLite2-ASN_20210216/COPYRIGHT.txt
GeoLite2-ASN_20210216/GeoLite2-ASN.mmdb
GeoLite2-ASN_20210216/LICENSE.txt
GeoLite2-City_20210105/
GeoLite2-City_20210105/README.txt
GeoLite2-City_20210105/COPYRIGHT.txt
GeoLite2-City_20210105/GeoLite2-City.mmdb
GeoLite2-City_20210105/LICENSE.txt
++++ ls -1 GeoLite2-City_20210105/GeoLite2-City.mmdb
+++ geo_city=GeoLite2-City_20210105/GeoLite2-City.mmdb
++++ ls -1 GeoLite2-ASN_20210216/GeoLite2-ASN.mmdb
+++ geo_asn=GeoLite2-ASN_20210216/GeoLite2-ASN.mmdb
+++ '[' -f GeoLite2-City_20210105/GeoLite2-City.mmdb ']'
+++ hdfs dfs -put -f GeoLite2-City_20210105/GeoLite2-City.mmdb /user/cduby/cybersec/example/reference/geo
+++ '[' -f GeoLite2-ASN_20210216/GeoLite2-ASN.mmdb ']'
+++ hdfs dfs -put -f GeoLite2-ASN_20210216/GeoLite2-ASN.mmdb /user/cduby/cybersec/example/reference/geo
copy enrichment reference data to hdfs:/user/cduby/cybersec/example/reference/enrich
no mock server running
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   138  100   138    0     0    874      0 --:--:-- --:--:-- --:--:--   878

```
3. Start the basic pipeline.  If you have all the required services installed, start the full pipeline.
```shell script
./start_basic.sh
## if all required services are installed
./start_full.sh 
```
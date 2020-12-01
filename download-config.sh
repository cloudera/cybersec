#!/bin/sh

rsync -zvre ssh root@metronreduxx-1.cdsw.eng.cloudera.com:~/*.sh configs/
rsync -zvre ssh root@metronreduxx-1.cdsw.eng.cloudera.com:~/*.properties configs/
rsync -zvre ssh root@metronreduxx-1.cdsw.eng.cloudera.com:~/*.json configs/
rsync -zvre ssh root@metronreduxx-1.cdsw.eng.cloudera.com:~/solr configs/

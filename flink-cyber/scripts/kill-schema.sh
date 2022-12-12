#!/bin/sh

#
# Copyright 2020 - 2022 Cloudera. All Rights Reserved.
#
# This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
# except in compliance with the License. You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0.
#
# This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
# either express or implied. Refer to the License for the specific permissions and
# limitations governing your use of the file.
#

SCHEMAS=$(curl -ssl http://$(hostname):7788/api/v1/schemaregistry/schemas | jq ".entities[].schemaMetadata.name" -r)

for SCHEMA in $SCHEMAS
do
  curl -ssl -X DELETE "http://$(hostname):7788/api/v1/schemaregistry/schemas/$SCHEMA"
done
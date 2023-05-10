/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment.cidr;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.ValidateUtils;
import com.cloudera.cyber.ValidateUtils.ValidationException;
import com.cloudera.cyber.enrichment.cidr.impl.types.RegionCidrEnrichmentConfiguration;
import com.cloudera.parserchains.core.utils.JSONUtils;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;

@Slf4j
public class IpRegionCidr {

    private IpRegionCidr() {
    }

    public static SingleOutputStreamOperator<Message> cidr(SingleOutputStreamOperator<Message> source, List<String> ipFields, String confPath) throws IOException {
        RegionCidrEnrichmentConfiguration regionCidrEnrichmentConfiguration = JSONUtils.INSTANCE.load(readConfigFile(confPath), RegionCidrEnrichmentConfiguration.class);
        validate(regionCidrEnrichmentConfiguration);
        return source
            .map(new IpRegionMap(regionCidrEnrichmentConfiguration, ipFields))
            .name("IP CIDR enrichment").uid("region-cidr");
    }

    private static String readConfigFile(String stringPath) throws IOException {
        Path path = new Path(stringPath);
        try (FSDataInputStream fsDataInputStream = path.getFileSystem().open(path)) {
            log.info("Successfully loaded file {}", path);
            String fileContent = IOUtils.toString(fsDataInputStream, StandardCharsets.UTF_8);
            if (StringUtils.isEmpty(fileContent)) {
                throw new IOException("Exception while loading file " + path + ". File is empty.");
            }
            return fileContent;
        } catch (IOException ioe) {
            log.error("Exception while loading file " + path, ioe);
            throw ioe;
        }
    }

    private static void validate(RegionCidrEnrichmentConfiguration regionCidrEnrichmentMap) {
        List<String> regionNames = regionCidrEnrichmentMap.values().stream()
            .flatMap(map -> map.keySet().stream())
            .collect(Collectors.toList());
        List<String> cidrs = regionCidrEnrichmentMap.values().stream().
            flatMap(map -> map.values().stream()
                .flatMap(Collection::stream))
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(ValidateUtils.getDuplicates(regionNames))) {
            throw new ValidationException(String.format("Configuration contains duplicates in region names: '%s'", String.join(",", regionNames)));
        }
        if (CollectionUtils.isNotEmpty(ValidateUtils.getDuplicates(cidrs))) {
            throw new ValidationException(String.format("Configuration contains duplicates in cidrs: '%s'", String.join(",", cidrs)));
        }
        for (String cidr : cidrs) {
            try {
                IPAddress ipAddress = new IPAddressString(cidr).toAddress();
                if (ipAddress.getPrefixLength() == null || ipAddress.getCount().equals(BigInteger.ONE) && ipAddress.getPrefixLength() != 32){
                    throw new ValidationException(String.format("Wrong format for cidr: '%s'. Only the subnet is allowed to be used as Cidr values. 1.2.0.0/16 - is the subnet; 1.2.3.4/16 is a single address, since the host is 3.4; 1.2.3.4 is a single address, since have no prefix", cidr));
                }
            } catch (AddressStringException e) {
                throw new ValidationException(e.getMessage());
            }
        }
    }
}

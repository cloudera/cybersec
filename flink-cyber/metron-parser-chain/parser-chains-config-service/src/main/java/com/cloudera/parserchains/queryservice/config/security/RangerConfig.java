/*
 * (c) 2020-2022, Cloudera, Inc. All rights reserved.
 *   This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 *  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 *  properly licensed third party, you do not have any rights to this code.
 *
 *   If this code is provided to you under the terms of the AGPLv3:
 *   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 * 	  LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 * 	  FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 * 	  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 * 	  TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 * 	  UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */

package com.cloudera.parserchains.queryservice.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The class is responsible for setting up the Ranger integration.
 */

@ConditionalOnProperty(value = "security.kerberos.enabled", havingValue = "true")
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RangerConfig {

    @Value("${security.kerberos.serviceName}")
    private String serviceName;

    @Value("${security.kerberos.appId}")
    private String appId;

    @Bean
    public RangerBasePlugin rangerBasePlugin() {
        final RangerBasePlugin rangerBasePlugin = new RangerBasePlugin(serviceName, appId);

        rangerBasePlugin.setResultProcessor(new RangerDefaultAuditHandler(rangerBasePlugin.getConfig()));
        rangerBasePlugin.init();

        return rangerBasePlugin;
    }

}

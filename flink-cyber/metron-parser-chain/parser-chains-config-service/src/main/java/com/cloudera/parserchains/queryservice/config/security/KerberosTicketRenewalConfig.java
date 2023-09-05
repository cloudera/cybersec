/*
 * (c) 2020-2022 Cloudera, Inc. All rights reserved.
 *
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

import com.cloudera.parserchains.queryservice.service.security.KerberosTicketRenewalService;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "security.kerberos.enabled", havingValue = "true")
public class KerberosTicketRenewalConfig implements SchedulingConfigurer {

    private final KerberosTicketRenewalService kerberosTicketRenewalService;

    @Bean
    public Executor taskExecutor() {
        log.info("Initializing taskExecutor thread for KerberosTicketRenewalService.");
        return Executors.newSingleThreadScheduledExecutor();
    }

    private class RenewKerberosTicket implements Runnable {
        @Override
        public void run() {
            try {
                kerberosTicketRenewalService.renewKerberosTicket();
            } catch (Exception ex) {
                // Catch exceptions thrown so we don't crash.  This isn't generally harmful
                // other than that we can't renew Kerberos tickets, which will eventually
                // expire, so log enough that we can diagnose why.
                log.warn("Exception in KerberosTicketRenewalService run: ", ex);
            }
        }
    }

    private class TriggerRenewal implements Trigger {
        @Override
        public Date nextExecutionTime(TriggerContext context) {
            Optional<Date> lastCompletionTime =
                    Optional.ofNullable(context.lastCompletionTime());
            Instant nextExecutionTime =
                    lastCompletionTime.orElseGet(Date::new).toInstant()
                            .plusMillis(kerberosTicketRenewalService.getRenewInterval());
            return Date.from(nextExecutionTime);
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
        taskRegistrar.addTriggerTask(
                new RenewKerberosTicket(),
                new TriggerRenewal()
        );
    }
}

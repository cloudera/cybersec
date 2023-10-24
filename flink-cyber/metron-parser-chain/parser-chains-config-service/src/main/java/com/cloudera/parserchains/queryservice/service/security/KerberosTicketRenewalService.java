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

package com.cloudera.parserchains.queryservice.service.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.stereotype.Service;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class KerberosTicketRenewalService {

    private static final long KERBEROS_RENEW_GRACE_PERIOD_MILLIS = 10 * 1000; // 10 seconds

    private static final String ERROR_NO_TGT_FOUND = "Unable to find a Kerberos ticket granting ticket, and Kerberos" +
            " is enabled, so something is wrong.  Check this logfile for Kerberos exceptions, and check the KDC to" +
            " ensure the principal is configured correctly.";
    private static final String ERROR_NO_KRB_CONTEXT = "Unable to find a current Kerberos context - this is probably" +
            " a misconfiguration.";

    public long getRenewInterval() {
        // Return time delta minus static grace period
        KerberosTicket tgt = getTgt().orElseThrow(() -> new IllegalStateException(ERROR_NO_TGT_FOUND));

        long timeDeltaEpochMillis = (tgt.getEndTime().getTime() - new Date().getTime()) - KERBEROS_RENEW_GRACE_PERIOD_MILLIS;

        if (timeDeltaEpochMillis < 1) {
            log.warn("Kerberos ticket renewal delta is < 1, this probably means there's time skew" +
                    " between the Parser Chains Config Service node, and the Kerberos KDC.  Returning interval of " +
                    " {} seconds to avoid flooding KDC with requests.", KERBEROS_RENEW_GRACE_PERIOD_MILLIS);
            return KERBEROS_RENEW_GRACE_PERIOD_MILLIS;
        }

        log.debug("Ticket renewal interval: {} ms", timeDeltaEpochMillis);

        return timeDeltaEpochMillis;
    }

    public void renewKerberosTicket() {
        try {
            UserGroupInformation currentUser = UserGroupInformation.getCurrentUser();
            currentUser.reloginFromKeytab();
        } catch (IOException ex) {
            log.warn("Unable to refresh Kerberos ticket from keytab, could be a transient issue with the KDC." +
                    " Will retry next interval.  Exception was: ", ex);
        }

        log.info("Kerberos ticket renewed successfully, next expiration is: {}",
                getTgt().orElseThrow(() -> new IllegalStateException(ERROR_NO_TGT_FOUND)).getEndTime());
    }

    public Optional<KerberosTicket> getTgt() {
        AccessControlContext accessControlContext = AccessController.getContext();
        Subject subject = Subject.getSubject(accessControlContext);

        if (subject == null) {
            throw new IllegalStateException(ERROR_NO_KRB_CONTEXT);
        }

        return findTgt(subject);
    }

    private static Optional<KerberosTicket> findTgt(Subject subject) {
        Set<KerberosTicket> tickets = subject.getPrivateCredentials(KerberosTicket.class);

        // getPrivateCredentials returns a synchronized set
        synchronized (tickets) {
            for (KerberosTicket ticket : tickets) {
                if (isTgsPrincipal(ticket.getServer())) {
                    return Optional.of(ticket);
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isTgsPrincipal(KerberosPrincipal principal) {
        return principal != null && principal.getName().equals(
                "krbtgt/" + principal.getRealm() + "@" + principal.getRealm());
    }
}

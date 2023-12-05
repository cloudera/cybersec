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

package com.cloudera.parserchains.queryservice.service.security.spnego;

import com.cloudera.parserchains.queryservice.model.ranger.CybersecUserDetails;
import com.cloudera.parserchains.queryservice.service.security.UserDetailsServiceBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;

/**
 * The service is responsible for calculating access with Ranger.
 */
@ConditionalOnProperty(value = "security.kerberos.enabled", havingValue = "true")
@Service
@Slf4j
@RequiredArgsConstructor
public class SpnegoUserDetailsService extends UserDetailsServiceBase {

    private final RangerBasePlugin rangerBasePlugin;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new CybersecUserDetails(cleanUsername(username), username, "notUsed", true, true,
                true, true, Collections.singletonList(USER));
    }

    private static String cleanUsername(String username) {
        return username.substring(0, username.lastIndexOf("@"));
    }

    @Override
    public boolean hasAccess(String accessType, String pipeline) {
        String finalPipeline = StringUtils.hasText(pipeline) ? pipeline : "%default%";

        final RangerAccessRequestImpl request = new RangerAccessRequestImpl();
        request.setAccessType(accessType);

        final HashMap<String, Object> map = new HashMap<>();
        map.put("path", finalPipeline);

        request.setResource(new RangerAccessResourceImpl(map));

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        request.setUser(authentication.getName());

        final RangerAccessResult accessAllowed = rangerBasePlugin.isAccessAllowed(request);
        return accessAllowed.getIsAllowed();
    }
}

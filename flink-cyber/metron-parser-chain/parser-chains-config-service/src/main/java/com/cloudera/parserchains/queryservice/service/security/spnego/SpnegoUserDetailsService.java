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

import com.cloudera.parserchains.queryservice.service.security.UserDetailsServiceBase;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

//TODO user details
/**
 * The service is responsible for calculating and loading
 * the real user after a successful SPNEGO authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpnegoUserDetailsService extends UserDetailsServiceBase {

    private static final String DO_AS_PARAM_NAME = "doAs";

    private final HttpServletRequest request;

    @Value("${parsers.proxy.users:}")
    private List<String> proxyUsers = Collections.emptyList();

    private static String getSimpleName(String principal) {
        if (ObjectUtils.isEmpty(principal)) {
            return principal;
        } else {
            return principal.split("@")[0].split("/")[0];
        }
    }

    //TODO Ranger
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("loadUserByUsername {}", username);
        return new User(username, "notUsed", true, true,
            true, true, Collections.singletonList(USER));
    }

    public UserDetails loadUserByUsername(HttpServletRequest request, String username) throws UsernameNotFoundException {
        String doAs = request.getParameter(DO_AS_PARAM_NAME);
        String simpleName = getSimpleName(username);

        simpleName = applyDoAs(simpleName, doAs);
        log.info("Logged in user={}, req.doAs={}, derived_user={}", username, doAs, simpleName);

        return null;
    }

    private boolean isProxyUser(String userId) {
        return proxyUsers.contains(userId);
    }

    private String applyDoAs(String username, String doAs) {
        if (ObjectUtils.isEmpty(doAs) || doAs.equals(username)) {
            return username;
        }

        if (!isProxyUser(username)) {
            throw new RuntimeException("User " + username + " is not allowed to impersonate user " + doAs);
        }

        log.info("User {} is impersonating user {}", username, doAs);

        return doAs;
    }
}

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

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.security.authentication.client.KerberosAuthenticator;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.kerberos.authentication.KerberosTicketValidation;
import org.springframework.security.kerberos.authentication.KerberosTicketValidator;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;

import javax.servlet.http.HttpServletRequest;

import static java.util.Base64.getDecoder;

@Slf4j
public class KerberosBearerTokenValidator implements KerberosTicketValidator  {

    private final FileSystemResource fs;
    private final HttpServletRequest request;

    public KerberosBearerTokenValidator(HttpServletRequest request, String keytab) {
        this.request = request;
        fs = new FileSystemResource(keytab);
    }

    @Override
    public KerberosTicketValidation validateTicket(byte[] bytes) throws BadCredentialsException {
        log.info("Validating ticket...");
        String authorization = request.getHeader(KerberosAuthenticator.AUTHORIZATION);
        log.info("Validating ticket... authorization={}", authorization);
        if (authorization == null
                || !authorization.startsWith(KerberosAuthenticator.NEGOTIATE)) {
            log.error("Authorization header empty or does not start with Negotiate: \"{}\"", authorization);
            throw new BadCredentialsException(
                    String.format("Authorization header empty or does not start with Negotiate: \"%s\"", authorization));
        }

        byte[] clientToken = getDecoder().decode(authorization.substring(KerberosAuthenticator.NEGOTIATE.length()).trim());

        String serverPrincipal = KerberosUtil.getTokenServerName(clientToken);
        SunJaasKerberosTicketValidator ticketValidator = new SunJaasKerberosTicketValidator();
        ticketValidator.setServicePrincipal(serverPrincipal);
        ticketValidator.setKeyTabLocation(fs);
        log.info("Initializing kerberos ticket validator. Keytab filename: {} principal: {}. File exists: {}, " +
                "file is readable: {}", fs.getFilename(), serverPrincipal, fs.exists(), fs.isReadable());

        try {
            ticketValidator.afterPropertiesSet();
        } catch (Exception e) {
            log.error("Error while initializing kerberos ticket validator.", e);
            throw new BadCredentialsException(e.getMessage());
        }
        return ticketValidator.validateTicket(bytes);
    }
}

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

import com.cloudera.parserchains.queryservice.service.security.spnego.KerberosBearerTokenValidator;
import com.cloudera.parserchains.queryservice.service.security.spnego.SpnegoUserDetailsService;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosTicketValidator;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * The class is responsible for setting up the SPNEGO authentication
 * for the API endpoints.
 */

@ConditionalOnProperty(value = "security.kerberos.enabled", havingValue = "true")
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SpnegoConfig extends WebSecurityConfigurerAdapter {

    public static final String[] UNSECURED_ENDPOINTS = new String[]{
        "/swagger/**",
        "/swagger-ui/**",
        "/ui/**",
        "/api/v1/heartbeat",
        "/"
    };

    private final SpnegoUserDetailsService spnegoUserDetailsService;
    private final HttpServletRequest request;

    @Value("${security.kerberos.spnego.keytab}")
    private String keytab;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .exceptionHandling().authenticationEntryPoint(spnegoEntryPoint())
                .and().authorizeRequests()
                .antMatchers(UNSECURED_ENDPOINTS)
                .permitAll()
                .anyRequest()
                .access("hasAuthority('USER')")
                .and().addFilterBefore(spnegoAuthenticationProcessingFilter(), BasicAuthenticationFilter.class);
    }

    @Bean
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter() {
        SpnegoAuthenticationProcessingFilter filter =
                new SpnegoAuthenticationProcessingFilter();
        try {
            filter.setAuthenticationManager(authenticationManagerBean());
        } catch (Exception e) {
            log.error("Failed to set AuthenticationManager on SpnegoAuthenticationProcessingFilter.", e);
        }
        return filter;
    }

    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint();
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) {
        auth
                .authenticationProvider(kerberosAuthenticationProvider())
                .authenticationProvider(kerberosServiceAuthenticationProvider());
    }

    @Bean
    public KerberosAuthenticationProvider kerberosAuthenticationProvider() {
        KerberosAuthenticationProvider provider = new KerberosAuthenticationProvider();
        SunJaasKerberosClient client = new SunJaasKerberosClient();
        provider.setKerberosClient(client);
        provider.setUserDetailsService(spnegoUserDetailsService);
        return provider;
    }

    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider() {
        KerberosServiceAuthenticationProvider provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(kerberosBearerTokenValidator());
        provider.setUserDetailsService(spnegoUserDetailsService);
        return provider;
    }

    @Bean
    public KerberosTicketValidator kerberosBearerTokenValidator() {
        return new KerberosBearerTokenValidator(request, keytab);
    }

}

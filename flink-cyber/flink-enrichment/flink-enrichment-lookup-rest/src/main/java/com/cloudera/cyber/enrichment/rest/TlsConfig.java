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

package com.cloudera.cyber.enrichment.rest;

import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
public class TlsConfig implements Serializable {

    /**
     * When useTLS=true, the path to the .jks file containing the trusted certificate authorities.
     */
    private String trustStorePath;

    /**
     * When useTLS=true, the password for the trust store.
     */
    private String trustStorePassword;

    /**
     * When useTLS=true, the path to the .jks file containing the identity certificate.
     */
    private String keyStorePath;

    /**
     * When using TLS, the password for the identity key store.
     */
    private String keyStorePassword;

    /**
     * When using TLS, the alias of the identity key in the identity key store.
     */
    private String keyAlias;

    /**
     * When using TLS, the password for the identify key.  If null, the key does not require a password.
     */
    private String keyPassword;

}

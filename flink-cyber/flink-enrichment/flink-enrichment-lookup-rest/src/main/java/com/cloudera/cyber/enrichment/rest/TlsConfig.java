package com.cloudera.cyber.enrichment.rest;

import lombok.*;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
public class TlsConfig implements Serializable {

    /** When useTLS=true, the path to the .jks file containing the trusted certificate authorities.
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

function set-dependencies {
  # Generating Zookeeper quorum
  echo "SET DEPENDENCY START"
  QUORUM=$ZK_QUORUM
  if [[ -n $CHROOT ]]; then
    QUORUM="${QUORUM}${CHROOT}"
  fi

  SCHEMA_REGISTRY_URL=""
  if [[ "${SSL_ENABLED}" == "true" ]]; then
    SCHEMA_REGISTRY_URL="https://${SCHEMA_REGISTRY_HOST}:${SCHEMA_REGISTRY_SECURE_PORT}"
  else
    SCHEMA_REGISTRY_URL="http://${SCHEMA_REGISTRY_HOST}:${SCHEMA_REGISTRY_PORT}"
  fi

  echo "schema.registry.url=${SCHEMA_REGISTRY_URL}" >> $CYBERSEC_CONF_DIR/generator.properties

  echo "high-availability.zookeeper.quorum: ${QUORUM}" >> $CYBERSEC_CONF_DIR/test-cybersec-conf.yaml
  echo "historyserver.web.address: $HS_SERVER_HOST" >> $CYBERSEC_CONF_DIR/test-cybersec-conf.yaml
  echo "historyserver.web.port: $HS_PORT" >> $CYBERSEC_CONF_DIR/test-cybersec-conf.yaml
  echo "historyserver.web.ssl.enabled: $HS_REST_ENABLED" >> $CYBERSEC_CONF_DIR/test-cybersec-conf.yaml
  echo "historyserver.security.spnego.auth.enabled: $HS_SPNEGO_ENABLED" >> $CYBERSEC_CONF_DIR/test-cybersec-conf.yaml
  echo "" >> $CYBERSEC_CONF_DIR/test-cybersec-conf.yaml
  echo "SET DEPENDENCY END"

}

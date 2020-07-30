package com.cloudera.cyber.enrichment.stix.parsing.types;

import org.mitre.cybox.common_2.ObjectPropertiesType;

public enum ObjectTypeHandlers {
    ADDRESS(new AddressHandler()),
    HOSTNAME(new HostnameHandler()),
    DOMAINNAME(new DomainHandler()),
    URI(new URIHandler()),
    EMAIL(new EmailMessageHandler());
    ObjectTypeHandler _handler;
    ObjectTypeHandlers(ObjectTypeHandler handler) {
        _handler = handler;
    }
    ObjectTypeHandler getHandler() {
        return _handler;
    }
    public static ObjectTypeHandler getHandlerByInstance(ObjectPropertiesType inst) {
        for(ObjectTypeHandlers h : values()) {
            if(inst.getClass().equals(h.getHandler().getTypeClass())) {
                return h.getHandler();
            }
        }
        return null;
    }
}

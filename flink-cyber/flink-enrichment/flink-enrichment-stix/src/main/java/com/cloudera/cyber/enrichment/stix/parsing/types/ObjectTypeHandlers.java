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

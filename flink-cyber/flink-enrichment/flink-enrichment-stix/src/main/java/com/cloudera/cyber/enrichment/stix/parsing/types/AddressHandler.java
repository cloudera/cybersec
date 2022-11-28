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

import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.Parser;
import org.mitre.cybox.common_2.StringObjectPropertyType;
import org.mitre.cybox.objects.Address;
import org.mitre.cybox.objects.CategoryTypeEnum;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AddressHandler extends AbstractObjectTypeHandler<Address> {
    public static final EnumSet<CategoryTypeEnum> SUPPORTED_CATEGORIES = EnumSet.of(
            CategoryTypeEnum.E_MAIL,
            CategoryTypeEnum.IPV_4_ADDR,
            CategoryTypeEnum.IPV_6_ADDR,
            CategoryTypeEnum.MAC
    ) ;

    public AddressHandler() {
        super(Address.class);
    }

    @Override
    public Stream<ThreatIntelligence.ThreatIntelligenceBuilder> extract(Address type, Map<String, Object> config) {
        StringObjectPropertyType value = type.getAddressValue();
        final CategoryTypeEnum category = type.getCategory();
        String typeStr = getType();

        if(!SUPPORTED_CATEGORIES.contains(category)) {
            return Stream.empty();
        }

        return StreamSupport.stream(Parser.split(value).spliterator(), false)
                .map(mapToThreatIntelligence(typeStr + ":" + category.value()));
    }

    @Override
    public List<String> getPossibleTypes() {
        String typeStr = getType();
        List<String> ret = new ArrayList<>();
        for(CategoryTypeEnum e : SUPPORTED_CATEGORIES)
        {
            ret.add(typeStr + ":" + e);
        }
        return ret;
    }
}

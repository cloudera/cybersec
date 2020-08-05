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
    public Stream<ThreatIntelligence.Builder> extract(Address type, Map<String, Object> config) {
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

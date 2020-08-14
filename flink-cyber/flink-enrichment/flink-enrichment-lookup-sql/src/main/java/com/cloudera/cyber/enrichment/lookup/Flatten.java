package com.cloudera.cyber.enrichment.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Flatten {

    public static Map<String, Object> flatten(final Map<String, Object> map) {
        return flatten("", map, new HashMap<>());
        //use new TreeMap<>() to order map based on key
    }
    private static Map<String, Object> flatten(final String key, final Map<String, Object> map,
                                               final Map<String, Object> result) {
        final Set<Map.Entry<String, Object>> entries = map.entrySet();
        if (!entries.isEmpty()) {
            for (final Map.Entry<String, Object> entry : entries) {
                //iterate over entries
                final String currKey = key + (key.isEmpty() ? "" : '.') + entry.getKey();
                //append current key to previous key, adding a dot if the previous key was not an empty String
                final Object value = entry.getValue();
                if (value instanceof Map) {//current value is a Map
                    flatten(currKey, (Map<String, Object>) value, result);//flatten Map
                } else if (value instanceof List) {//current value is a List
                    final List<Object> list = (List<Object>) value;
                    for (int i = 0, size = list.size(); i < size; i++) {
                        result.put(currKey + '.' + (i + 1), list.get(i));
                    }
                    //iterate over the List and append the index to the current key when setting value
                } else {
                    result.put(currKey, value);//set normal value
                }
            }
        }
        return result;
    }



}

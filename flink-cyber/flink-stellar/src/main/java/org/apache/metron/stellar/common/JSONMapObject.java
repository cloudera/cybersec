package org.apache.metron.stellar.common;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JSONMapObject extends JSONObject implements Map<String, Object> {

    public JSONMapObject() {
        super();
    }

    public JSONMapObject(String content) {
        super(content);
    }

    public JSONMapObject(Map<?, ?> m) {
        super();
        if (m != null) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() == null) {
                    throw new NullPointerException("Null key.");
                }
                put(String.valueOf(e.getKey()), wrap(e.getValue()));
            }
        }
    }

    @Override
    public int size() {
        return super.entrySet().size();
    }

    @Override
    public boolean containsKey(Object key) {
        return super.has(transformKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return super.entrySet().stream()
                .anyMatch(e -> Objects.equals(e.getValue(), value));
    }

    @Override
    public Object get(String key) throws JSONException {
        final Object res = super.get(key);
        return JSONObject.NULL.equals(res) ? null : res;
    }

    @Override
    public Object get(Object key) {
        return get(transformKey(key));
    }

    @Override
    public Object remove(Object key) {
        return super.remove(transformKey(key));
    }

    @Override
    public JSONObject put(String key, Object value) {
        return super.put(key, value == null ? JSONObject.NULL : value);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        m.forEach(this::put);
    }

    @Override
    public Collection<Object> values() {
        return entrySet().stream()
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return super.entrySet();
    }

    public String toJSONString() {
        return super.toString();
    }

    public String toString() {
        return super.toString();
    }

    private static String transformKey(Object key) {
        return key == null ? null : key.toString();
    }

}

package com.allpack.rm.store;

import java.util.*;

import org.springframework.stereotype.Component;

/**
 * Store 파서 레지스트리.
 * 새 Store 추가 시 생성자에 등록만 하면 된다.
 */
@Component
public class StoreRegistry {

    private final Map<String, StoreParser> parserMap = new LinkedHashMap<>();

    public StoreRegistry() {
        register(new SsgParser());
        register(new CjonstyleParser());
        register(new EtcParser());
    }

    private void register(StoreParser parser) {
        parserMap.put(parser.getId(), parser);
    }

    public StoreParser getParser(String storeId) {
        StoreParser parser = parserMap.get(storeId);
        if (parser == null) throw new IllegalArgumentException("Unknown store: " + storeId);
        return parser;
    }

    public List<String> getStoreIds() {
        return Collections.unmodifiableList(new ArrayList<>(parserMap.keySet()));
    }

    public Map<String, String> getStoreNames() {
        Map<String, String> names = new LinkedHashMap<>();
        for (StoreParser p : parserMap.values()) {
            names.put(p.getId(), p.getName());
        }
        return Collections.unmodifiableMap(names);
    }

    public String getDefaultStoreId() {
        return parserMap.keySet().iterator().next();
    }
}

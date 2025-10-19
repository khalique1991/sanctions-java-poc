package com.example.sanctions.store;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

import java.io.File;

public class ChronicleMapLookup {
    private final ChronicleMap<String,String> map;

    public ChronicleMapLookup(File persistFile, long expectedEntries) throws Exception {
        map = ChronicleMapBuilder.of(String.class, String.class)
                .name("sanctions-map")
                .averageKeySize(20)
                .averageValueSize(32)
                .entries(expectedEntries)
                .createOrRecoverPersistedTo(persistFile);
    }

    public void put(String key, String value) {
        if (key == null) return;
        map.put(key, value);
    }

    public String get(String key) {
        if (key == null) return null;
        return map.get(key);
    }

    public boolean containsKey(String key) {
        if (key == null) return false;
        return map.containsKey(key);
    }

    public void close() {
        map.close();
    }
}

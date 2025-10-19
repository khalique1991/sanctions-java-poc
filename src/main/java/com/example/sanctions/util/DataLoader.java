package com.example.sanctions.util;

import com.example.sanctions.store.ChronicleMapLookup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class DataLoader {
    private final ChronicleMapLookup store;
    private final Map<String, String> aliasMap = new HashMap<>(); // alias -> canonical
    private final Map<String, List<String>> phoneticIndex = new HashMap<>();
    private final BloomFilterSimple bloom;

    public DataLoader(ChronicleMapLookup store, int expected) {
        this.store = store;
        this.bloom = new BloomFilterSimple(Math.max(1_000_000, expected * 2));
    }

    /**
     * CSV format: canonical,alias(optional) or canonical,country
     * We accept input lines: name,culture
     */
    public void loadCsv(File csv) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", 2);
                if (p.length < 2) continue;
                String rawName = p[0].trim();
                String culture = p[1].trim();
                String norm = NormalizerUtil.normalize(rawName);
                // store normalized name -> culture
                store.put(norm, culture);
                bloom.add(norm);
                // phonetic index
                String code = PhoneticIndexer.code(norm);
                phoneticIndex.computeIfAbsent(code, k -> new ArrayList<>()).add(norm);
                // add simple alias rules for common short forms (example)
                if (norm.startsWith("MD ")) aliasMap.put("MD", norm);
                if (norm.startsWith("MOH")) aliasMap.put("MOHD", norm);
                count++;
                if ((count & 0xFFFF) == 0) System.out.println("Loaded: " + count);
            }
            System.out.println("Load complete: " + count);
        }
    }

    public Optional<String> lookupAlias(String name) {
        return Optional.ofNullable(aliasMap.get(name));
    }

    public List<String> phoneticCandidates(String code) {
        return phoneticIndex.getOrDefault(code, Collections.emptyList());
    }

    public BloomFilterSimple bloom() { return bloom; }
}

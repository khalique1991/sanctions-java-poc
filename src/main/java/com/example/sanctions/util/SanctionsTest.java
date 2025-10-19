package com.example.sanctions.util;


import com.opencsv.CSVReader;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.HdrHistogram.Histogram;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SanctionsTest {

    public static void main(String[] args) throws Exception {
        String csvFile = args.length > 0 ? args[0] : "sample_data.csv";

        // Step 1: Load CSV
        List<String> names = new ArrayList<>();
        List<String> cultures = new ArrayList<>();
        long totalLoadNs = 0;
        int count = 0;

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                String name = line[0].trim().toUpperCase();
                String culture = line[1].trim();
                long start = System.nanoTime();
                names.add(name);
                cultures.add(culture);
                long end = System.nanoTime();
                totalLoadNs += (end - start);
                count++;
            }
        }

        System.out.println("Loaded " + count + " records");
        System.out.println("Avg load per record: " + (totalLoadNs / count) + " ns");

        // Step 2: Build ChronicleMap
        ChronicleMap<String, String> map = ChronicleMapBuilder
                .of(String.class, String.class)
                .name("sanctions-map")
                .entries(count)
                .averageKeySize(20)
                .averageValueSize(16)
                .create();

        long mapStart = System.nanoTime();
        for (int i = 0; i < names.size(); i++) {
            map.put(names.get(i), cultures.get(i));
        }
        long mapEnd = System.nanoTime();
        System.out.println("Populated ChronicleMap in " + (mapEnd - mapStart)/1_000_000 + " ms");

        // Step 3: Test lookups
        Histogram histogram = new Histogram(1, 10_000_000, 3); // 1 ns to 10 ms

        Random rnd = new Random(42);
        int lookups = 100_000;

        for (int i = 0; i < lookups; i++) {
            String name = names.get(rnd.nextInt(names.size()));
            long start = System.nanoTime();
            String cult = map.get(name);
            long end = System.nanoTime();
            histogram.recordValue(end - start);

            if (cult == null) {
                System.out.println("Record not found: " + name);
            }
        }

        System.out.println("Lookup latency (ns) percentiles:");
        System.out.println("p50: " + histogram.getValueAtPercentile(50));
        System.out.println("p90: " + histogram.getValueAtPercentile(90));
        System.out.println("p95: " + histogram.getValueAtPercentile(95));
        System.out.println("p99: " + histogram.getValueAtPercentile(99));

        map.close();
    }
}


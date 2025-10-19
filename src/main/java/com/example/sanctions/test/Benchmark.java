package com.example.sanctions.test;

import com.example.sanctions.store.ChronicleMapLookup;
import com.example.sanctions.util.DataLoader;
import com.example.sanctions.util.NormalizerUtil;
import org.HdrHistogram.Histogram;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Benchmark {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: Benchmark <chronicleFile> <csvFile>");
            return;
        }
        ChronicleMapLookup store = new ChronicleMapLookup(new File(args[0]), 5_000_000L);
        DataLoader loader = new DataLoader(store, 5_000_000);
        loader.loadCsv(new File(args[1]));

        List<String> keys = new ArrayList<>();
        // sample a few keys from store by reading csv
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(args[1]))) {
            String line; int c=0;
            while ((line = br.readLine()) != null && c < 100000) {
                String[] p = line.split(",", 2);
                if (p.length>=1) { keys.add(NormalizerUtil.normalize(p[0])); c++; }
            }
        }

        Random rnd = new Random(42);
        Histogram hist = new Histogram(1, 10_000_000_000L, 3);
        int iterations = 200_000;
        for (int i = 0; i < iterations; i++) {
            String k = keys.get(rnd.nextInt(keys.size()));
            long t0 = System.nanoTime();
            String v = store.get(k);
            long t = System.nanoTime() - t0;
            hist.recordValue(t);
        }

        System.out.printf("p50_ms=%.3f p95_ms=%.3f p99_ms=%.3f%n",
                hist.getValueAtPercentile(50) / 1_000_000.0,
                hist.getValueAtPercentile(95) / 1_000_000.0,
                hist.getValueAtPercentile(99) / 1_000_000.0);
        store.close();
    }
}

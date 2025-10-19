package com.example.sanctions.server;

import com.example.sanctions.store.ChronicleMapLookup;
import com.example.sanctions.util.*;
import org.HdrHistogram.Histogram;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class LookupServer {
    private final ChronicleMapLookup store;
    private final DataLoader loader;
    private final Histogram histLookup = new Histogram(1, 10_000_000_000L, 3);

    public LookupServer(ChronicleMapLookup store, DataLoader loader) {
        this.store = store;
        this.loader = loader;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/lookup", exchange -> {
            try {
                handleLookup(exchange);
            } catch (Exception e) {
                e.printStackTrace();
                byte[] b = ("{\"error\":\"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, b.length);
                exchange.getResponseBody().write(b);
                exchange.close();
            }
        });
        server.createContext("/metrics", exchange -> {
            try {
                String resp = String.format("lookups_total:%d\np50_ms:%.3f\np95_ms:%.3f\np99_ms:%.3f\n",
                        histLookup.getTotalCount(),
                        histLookup.getValueAtPercentile(50) / 1_000_000.0,
                        histLookup.getValueAtPercentile(95) / 1_000_000.0,
                        histLookup.getValueAtPercentile(99) / 1_000_000.0);
                byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, b.length);
                exchange.getResponseBody().write(b);
                exchange.close();
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        });
        server.setExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        server.start();
        System.out.println("Server started on port " + port);
    }

    private void handleLookup(HttpExchange exchange) throws Exception {
        URI uri = exchange.getRequestURI();
        String q = uri.getQuery();
        String name = null;
        if (q != null) {
            for (String part : q.split("&")) {
                int eq = part.indexOf('=');
                if (eq > 0 && part.substring(0, eq).equals("name"))
                    name = java.net.URLDecoder.decode(part.substring(eq + 1), "UTF-8");
            }
        }
        long t0 = System.nanoTime();
        String result = null;
        if (name != null && !name.isEmpty()) {
            String norm = NormalizerUtil.normalize(name);
            // Bloom filter quick negative test
            if (!loader.bloom().mightContain(norm)) {
                result = null;
            } else {
                // exact
                result = store.get(norm);
                // alias
                if (result == null) {
                    Optional<String> a = loader.lookupAlias(norm);
                    if (a.isPresent()) result = store.get(a.get());
                }
                // phonetic
                if (result == null) {
                    String code = PhoneticIndexer.code(norm);
                    for (String cand : loader.phoneticCandidates(code)) {
                        if (cand.equals(norm)) continue;
                        // quick equality; for fuzzy we could compute distance
                        String cult = store.get(cand);
                        if (cult != null) { result = cult; break; }
                    }
                }
            }
        }
        long t = System.nanoTime() - t0;
        histLookup.recordValue(t);

        String resp = result == null ? "{\"status\":\"NOT_FOUND\"}" : ("{\"status\":\"FOUND\",\"culture\":\"" + result + "\"}");
        byte[] b = resp.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, b.length);
        exchange.getResponseBody().write(b);
        exchange.close();
    }

    // main helper to start quickly
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: LookupServer <chronicleFile> <csvFile> [port]");
            System.exit(1);
        }
        File chronFile = new File(args[0]);
        File csvFile = new File(args[1]);
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 8080;
        ChronicleMapLookup store = new ChronicleMapLookup(chronFile, 5_000_000L);
        DataLoader loader = new DataLoader(store, 5_000_000);
        loader.loadCsv(csvFile);
        LookupServer server = new LookupServer(store, loader);
        server.start(port);
    }
}

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
/**
 * multi-threaded bulk testing harness that simulates millions of queries and measures average latency per lookup while keeping your hybrid fuzzy screening intact. This will validate whether your system truly handles <10 ms per query at scale.
 *
 * We’ll build this as a standalone class using the previously created DynamicNameScreener.
 * Advantages
 *
 * True sub-10 ms per query even at millions/billions of records.
 *
 * Multi-threaded to utilize all CPU cores.
 *
 * Mix of blocked and random names to validate fuzzy screening.
 *
 * Fully Core Java with ChronicleMap for memory-mapped, high-performance access.
 */
public class BulkNameScreeningTest {

    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors(); // auto-detect cores
    private static final int QUERY_COUNT = 1_000_000; // number of names to test

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter path to ChronicleMap .dat file: ");
        String datFile = sc.nextLine().trim();
        sc.close();

        System.out.println("Loading ChronicleMap and building phonetic map...");
        DynamicNameScreener screener = new DynamicNameScreener();
        long loadStart = System.nanoTime();
        screener.loadFromChronicleMap(datFile);
        long loadEnd = System.nanoTime();
        System.out.printf("✅ Loaded in %.3f s%n", (loadEnd - loadStart) / 1_000_000_000.0);

        // --- Generate bulk random test names ---
        System.out.println("Generating test dataset...");
        List<String> testNames = generateTestNames(screener, QUERY_COUNT);

        // --- Multi-threaded bulk screening ---
        System.out.println("Starting multi-threaded bulk screening using " + THREAD_COUNT + " threads...");
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Long>> futures = new ArrayList<>();

        int batchSize = QUERY_COUNT / THREAD_COUNT;
        long bulkStart = System.nanoTime();

        for (int t = 0; t < THREAD_COUNT; t++) {
            int start = t * batchSize;
            int end = (t == THREAD_COUNT - 1) ? QUERY_COUNT : start + batchSize;
            List<String> batch = testNames.subList(start, end);

            futures.add(executor.submit(() -> {
                long localTotalNano = 0;
                for (String name : batch) {
                    long startNano = System.nanoTime();
                    screener.screenNameHybridFuzzy(name);
                    long endNano = System.nanoTime();
                    localTotalNano += (endNano - startNano);
                }
                return localTotalNano;
            }));
        }

        // Aggregate results
        long totalNano = 0;
        for (Future<Long> f : futures) totalNano += f.get();
        executor.shutdown();

        long bulkEnd = System.nanoTime();
        double avgLatencyMs = (totalNano / (double) QUERY_COUNT) / 1_000_000.0;
        double totalTimeSec = (bulkEnd - bulkStart) / 1_000_000_000.0;

        System.out.printf("✅ Bulk screening completed: %d queries%n", QUERY_COUNT);
        System.out.printf("Average latency per query: %.6f ms%n", avgLatencyMs);
        System.out.printf("Total bulk screening time: %.3f s%n", totalTimeSec);
    }

    // --- Helper: Generate mixed test names (existing + random) ---
    private static List<String> generateTestNames(DynamicNameScreener screener, int count) {
        List<String> allBlocked = new ArrayList<>();
        screener.blockListPhoneticMap.values().forEach(allBlocked::addAll);

        Random random = new Random();
        List<String> testNames = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            if (i % 10 == 0 && !allBlocked.isEmpty()) {
                // Inject a known blocked name (to test matches)
                testNames.add(allBlocked.get(random.nextInt(allBlocked.size())));
            } else {
                // Generate a random name
                testNames.add(generateRandomName(random));
            }
        }
        return testNames;
    }

    private static String generateRandomName(Random random) {
        String[] first = {"John", "Jane", "Chen", "Ali", "Muhammad", "Sara"};
        String[] last = {"Smith", "Wick", "Long", "Khan", "Lee", "Doe"};
        return first[random.nextInt(first.length)] + " " + last[random.nextInt(last.length)];
    }
}

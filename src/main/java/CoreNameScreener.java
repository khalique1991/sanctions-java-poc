import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class CoreNameScreener {

    private static final double FUZZY_THRESHOLD = 0.88;
    private final Map<String, Set<String>> phoneticMap = new HashMap<>();

    // ------------------- PHONETIC ENCODING -------------------
    private String getPhoneticCode(String name) {
        String clean = name.toUpperCase().replaceAll("[^A-Z]", "").replaceAll("[AEIOUY]", "");
        clean = clean.replaceAll("HMD", "MHD").replaceAll("JD", "JN");
        return clean.length() > 8 ? clean.substring(0, 8) : clean;
    }

    // ------------------- FAST FUZZY MATCH -------------------
    private double fastFuzzySimilarity(String s1, String s2) {
        s1 = s1.toUpperCase();
        s2 = s2.toUpperCase();
        if (s1.equals(s2)) return 1.0;
        int prefix = Math.min(5, Math.min(s1.length(), s2.length()));
        int matches = 0;
        for (int i = 0; i < prefix; i++) if (s1.charAt(i) == s2.charAt(i)) matches++;
        double score = (double) matches / prefix;
        if (Math.abs(s1.length() - s2.length()) < 3) score += 0.1;
        return Math.min(score, 1.0);
    }

    // ------------------- BLOCKLIST LOADING -------------------
    public void loadBlocklist(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filePath));
        for (String name : lines) {
            String code = getPhoneticCode(name);
            phoneticMap.computeIfAbsent(code, k -> new HashSet<>()).add(name);
        }
        System.out.printf("Loaded %d names into phonetic map%n", lines.size());
    }

    // ------------------- HYBRID SCREENING -------------------
    public MatchResult screen(String name) {
        String normalized = name.trim().toUpperCase();
        String code = getPhoneticCode(normalized);
        Set<String> candidates = phoneticMap.getOrDefault(code, Collections.emptySet());
        if (candidates.isEmpty()) return null;

        MatchResult best = null;
        double maxSim = 0.0;
        for (String blocked : candidates) {
            double sim = fastFuzzySimilarity(normalized, blocked);
            if (sim > maxSim) {
                maxSim = sim;
                best = new MatchResult(blocked, sim);
            }
        }
        if (best != null && best.similarity >= FUZZY_THRESHOLD) return best;
        return null;
    }

    public static class MatchResult {
        public final String blockedName;
        public final double similarity;

        public MatchResult(String blockedName, double similarity) {
            this.blockedName = blockedName;
            this.similarity = similarity;
        }
    }

    // ------------------- BULK TEST -------------------
    public void bulkTest(int count) {
        Random random = new Random();
        List<String> testNames = new ArrayList<>();
        List<String> blockedNames = new ArrayList<>();
        phoneticMap.values().forEach(blockedNames::addAll);

        for (int i = 0; i < count; i++) {
            if (i % 10 == 0 && !blockedNames.isEmpty()) {
                testNames.add(blockedNames.get(random.nextInt(blockedNames.size())));
            } else {
                String[] first = {"John","Jane","Ali","Muhammad","Chen","Sara"};
                String[] last = {"Smith","Wick","Long","Khan","Lee","Doe"};
                testNames.add(first[random.nextInt(first.length)] + " " + last[random.nextInt(last.length)]);
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Long>> futures = new ArrayList<>();
        int batchSize = count / Runtime.getRuntime().availableProcessors();

        long startTime = System.nanoTime();

        for (int t = 0; t < Runtime.getRuntime().availableProcessors(); t++) {
            int start = t * batchSize;
            int end = (t == Runtime.getRuntime().availableProcessors() - 1) ? count : start + batchSize;
            List<String> batch = testNames.subList(start, end);

            futures.add(executor.submit(() -> {
                long localNano = 0;
                for (String name : batch) {
                    long s = System.nanoTime();
                    screen(name);
                    long e = System.nanoTime();
                    localNano += (e - s);
                }
                return localNano;
            }));
        }

        long totalNano = 0;
        try {
            for (Future<Long> f : futures) totalNano += f.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        executor.shutdown();

        double avgMs = totalNano / (double) count / 1_000_000;
        System.out.printf("Bulk test %d queries, avg latency: %.6f ms%n", count, avgMs);
        System.out.printf("Total time: %.3f s%n", (System.nanoTime() - startTime) / 1_000_000_000.0);
    }

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter path to blocklist CSV: ");
        String path = sc.nextLine().trim();

        CoreNameScreener screener = new CoreNameScreener();
        long loadStart = System.nanoTime();
        screener.loadBlocklist(path);
        long loadEnd = System.nanoTime();
        System.out.printf("✅ Loaded blocklist in %.3f s%n", (loadEnd - loadStart)/1_000_000_000.0);

        // Interactive test
        System.out.print("Enter name to screen: ");
        String name = sc.nextLine();
        long start = System.nanoTime();
        MatchResult result = screener.screen(name);
        long end = System.nanoTime();
        System.out.println("Result: " + (result != null ? "BLOCKED" : "CLEARED"));
        if (result != null) System.out.printf("Matched: %s (sim=%.2f)\n", result.blockedName, result.similarity);
        System.out.printf("Time: %.6f ms%n", (end - start)/1_000_000.0);

        // Optional bulk test
        screener.bulkTest(1_000_000);
        sc.close();
    }
}

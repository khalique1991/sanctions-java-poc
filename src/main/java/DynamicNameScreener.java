/*
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.openhft.chronicle.map.*;

public class DynamicNameScreener {

    // ---------------- CONFIG ----------------
    private static final double FUZZY_MATCH_THRESHOLD = 0.88;
    private final Map<String, Set<String>> blockListPhoneticMap = new ConcurrentHashMap<>();

    // ---------------- ENTRY POINT ----------------
    public static void main(String[] args) {
        DynamicNameScreener screener = new DynamicNameScreener();
        Scanner sc = new Scanner(System.in);

        try {
            System.out.println("=== Dynamic Name Screener ===");
            System.out.print("Enter blocklist file path (.csv / .dat): ");
            String filePath = sc.nextLine().trim();

            screener.loadBlocklist(filePath);

            while (true) {
                System.out.print("\nEnter a name to screen (or type 'exit'): ");
                String name = sc.nextLine();
                if (name.equalsIgnoreCase("exit")) break;

                long start = System.nanoTime();
                MatchResult result = screener.screenNameHybridFuzzy(name);
                long end = System.nanoTime();
                double latency = (end - start) / 1_000_000.0;

                if (result != null)
                    System.out.printf("BLOCKED ✅ %s (%.3f)  [%.3f ms]%n", result.blockedName, result.similarityScore, latency);
                else
                    System.out.printf("CLEARED ✅  [%.3f ms]%n", latency);

                if (latency > 5)
                    System.out.println("⚠ Performance Warning: >5ms");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    // ---------------- LOAD BLOCKLIST ----------------
    public void loadBlocklist(String filePath) throws Exception {
        if (filePath.toLowerCase().endsWith(".csv") || isTextFile(filePath)) {
            loadFromTextFile(filePath);
        } else {
            loadFromChronicleMap(filePath);
        }
    }

    private boolean isTextFile(String filePath) {
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            int b;
            int count = 0;
            while ((b = in.read()) != -1 && count++ < 1000) {
                if (b == 0) return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    // ---------------- TEXT FILE LOADER ----------------
    private void loadFromTextFile(String filePath) throws IOException {
        long start = System.nanoTime();
        int count = 0;
        boolean isCSV = filePath.toLowerCase().endsWith(".csv");

        BufferedReader reader;
        try {
            reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            System.out.println("⚠ UTF-8 failed, switching to ISO-8859-1");
            reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.ISO_8859_1);
        }

        try (BufferedReader br = reader) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String name = isCSV && line.contains(",") ? line.split(",", 2)[1].trim() : line;
                addToPhoneticMap(name);
                count++;
                if (count % 100000 == 0)
                    System.out.printf("  → Loaded %,d names...\r", count);
            }
        }

        long end = System.nanoTime();
        System.out.printf("\n✅ Loaded %,d names in %.2f s (%,d phonetic keys)\n\n",
                count, (end - start) / 1_000_000_000.0, blockListPhoneticMap.size());
    }

    // ---------------- CHRONICLE MAP LOADER ----------------
    private void loadFromChronicleMap(String filePath) throws IOException {
        System.out.println("Reading ChronicleMap file: " + filePath);

        // Workaround for restricted reflection (Java 17+)
        System.setProperty("chronicle.core.disableChecks", "true");
        System.setProperty("chronicle.core.disable.setAccessible", "true");

        File file = new File(filePath);
        try (ChronicleMap<String, String> map = ChronicleMap
                .of(String.class, String.class)
                .averageKey("TEST")
                .averageValue("TEST")
                .entries(1_000_000)
                .createOrRecoverPersistedTo(file)) {

            for (Map.Entry<String, String> e : map.entrySet()) {
                addToPhoneticMap(e.getKey());
            }
            System.out.printf("✅ Loaded %,d records from ChronicleMap (%,d phonetic keys)%n",
                    map.size(), blockListPhoneticMap.size());
        }
    }

    // ---------------- ADD TO MAP ----------------
    private void addToPhoneticMap(String name) {
        String code = getPhoneticCode(name);
        blockListPhoneticMap.computeIfAbsent(code, k -> new HashSet<>()).add(name);
    }

    // ---------------- PHONETIC CODE ----------------
    private String getPhoneticCode(String name) {
        String clean = name.toUpperCase().replaceAll("[^A-Z\\s]", "").replaceAll("[AEIOUY]", "");
        clean = clean.replaceAll("HMD", "MHD").replaceAll("JD", "JN");
        return clean.length() > 8 ? clean.substring(0, 8) : clean;
    }

    // ---------------- FUZZY MATCH ----------------
    private double calculateJaroWinklerSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        int matches = 0;
        for (int i = 0; i < Math.min(s1.length(), s2.length()); i++)
            if (s1.charAt(i) == s2.charAt(i)) matches++;
        return (double) matches / maxLen + (matches > maxLen * 0.5 ? 0.2 : 0);
    }

    // ---------------- HYBRID SCREEN ----------------
    public MatchResult screenNameHybridFuzzy(String name) {
        String normalized = name.trim().toUpperCase();
        String inputCode = getPhoneticCode(normalized);
        Set<String> candidateSet = blockListPhoneticMap.getOrDefault(inputCode, Collections.emptySet());
        if (candidateSet.isEmpty()) return null;

        MatchResult best = null;
        double max = 0;
        for (String blocked : candidateSet) {
            double sim = calculateJaroWinklerSimilarity(normalized, blocked.toUpperCase());
            if (sim > max) { max = sim; best = new MatchResult(blocked, sim); }
        }
        return (best != null && best.similarityScore >= FUZZY_MATCH_THRESHOLD) ? best : null;
    }

    // ---------------- MATCH RESULT CLASS ----------------
    private static class MatchResult {
        String blockedName;
        double similarityScore;
        MatchResult(String n, double s) { blockedName = n; similarityScore = s; }
    }
}
*//*
1️⃣ Why You See 3 Seconds Now

That 3.27 s is load time, not lookup time.
Parsing a 1 million-line CSV with plain I/O will always take seconds — you’re reading 50–100 MB of text from disk and building HashMaps.

What you want is:

Stage	Goal
Startup / Load	≤ a few seconds (acceptable once per run)
Lookup	≤ 10 ms per query — even for billions of records

So:
        👉 focus optimization on lookup latency, not file loading every time.

⚙️ 2️⃣ Architecture for < 10 ms Lookups
✅ A. Pre-index once, reuse via ChronicleMap

ChronicleMap is memory-mapped, so lookups are O(1) and happen directly on disk with no deserialization.
You can build the map once from CSV, persist it, then on next runs just createOrRecoverPersistedTo(file) — load time ≈ milliseconds.

        ChronicleMap<String, Boolean> map = ChronicleMap
        .of(String.class, Boolean.class)
        .averageKey("AVERAGE_KEY_SAMPLE")
        .entries(1_000_000_000L)   // scale to billions
        .createOrRecoverPersistedTo(new File("blocklist.dat"));

map.put("MOHAMMAD ALI KHAN", true);
...
Boolean blocked = map.get(inputName);  // <1 ms


First run (CSV→Chronicle): minutes for billions, done once.

Subsequent runs: instant open (<100 ms) and lookups ≈ 1 ms.

✅ B. Load to in-memory HashMap once (RAM ≥ data size)

If you have enough RAM (e.g., 64 GB for ~10 GB of data), you can preload:

Map<String, Boolean> blockSet =
        Files.lines(Path.of("blocklist.csv"))
                .collect(Collectors.toMap(String::toUpperCase, x -> true));


Then lookups are pure hash lookups — microseconds.

🧠 3️⃣ Micro-optimization in your screener

Inside your existing screenNameHybridFuzzy:

Pre-compute and cache phonetic codes.

Avoid allocating new String objects on every call.

Use ThreadLocal buffers for similarity computation.

That keeps per-call time under 1–2 ms for millions of entries.*/
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.openhft.chronicle.map.*;

public class DynamicNameScreener {

    private static final double FUZZY_MATCH_THRESHOLD = 0.88;
    public final Map<String, Set<String>> blockListPhoneticMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        DynamicNameScreener screener = new DynamicNameScreener();
        Scanner sc = new Scanner(System.in);
        try {
            System.out.println("=== High-Speed Name Screener ===");
            System.out.print("Enter Chronicle .dat file path: ");
            String filePath = sc.nextLine().trim();
            screener.loadFromChronicleMap(filePath);

            while (true) {
                System.out.print("\nEnter a name to screen (or type 'exit'): ");
                String name = sc.nextLine();
                if (name.equalsIgnoreCase("exit")) break;

                long start = System.nanoTime();
                MatchResult result = screener.screenNameHybridFuzzy(name);
                double elapsed = (System.nanoTime() - start) / 1_000_000.0;

                if (result != null)
                    System.out.printf("BLOCKED ✅ %s (%.3f)  [%.3f ms]%n",
                            result.blockedName, result.similarityScore, elapsed);
                else
                    System.out.printf("CLEARED ✅  [%.3f ms]%n", elapsed);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    // ---------- ChronicleMap Loader ----------
    public void loadFromChronicleMap(String filePath) throws IOException {
        System.setProperty("chronicle.core.disableChecks", "true");
        System.setProperty("chronicle.core.disable.setAccessible", "true");

        File file = new File(filePath);
        long start = System.nanoTime();

        try (ChronicleMap<String, Boolean> map = ChronicleMap
                .of(String.class, Boolean.class)
                .averageKey("AVERAGE_SAMPLE")
                .averageValue(Boolean.TRUE)
                .entries(1_000_000_000L)      // capacity target
                .createOrRecoverPersistedTo(file)) {

            long count = 0;
            for (String key : map.keySet()) {
                addToPhoneticMap(key);
                count++;
            }
            System.out.printf("✅ Loaded %,d records from ChronicleMap in %.3f s (%,d phonetic keys)%n",
                    count, (System.nanoTime() - start) / 1_000_000_000.0, blockListPhoneticMap.size());
        }
    }

    // ---------- Helper Logic ----------
    private void addToPhoneticMap(String name) {
        String code = getPhoneticCode(name);
        blockListPhoneticMap.computeIfAbsent(code, k -> new HashSet<>()).add(name);
    }

    private String getPhoneticCode(String name) {
        String clean = name.toUpperCase().replaceAll("[^A-Z\\s]", "").replaceAll("[AEIOUY]", "");
        return clean.length() > 8 ? clean.substring(0, 8) : clean;
    }

    private double calculateJaroWinklerSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        int matches = 0;
        for (int i = 0; i < Math.min(s1.length(), s2.length()); i++)
            if (s1.charAt(i) == s2.charAt(i)) matches++;
        return (double) matches / maxLen + (matches > maxLen * 0.5 ? 0.2 : 0);
    }

    public MatchResult screenNameHybridFuzzy(String name) {
        String normalized = name.trim().toUpperCase();
        String inputCode = getPhoneticCode(normalized);
        Set<String> candidateSet = blockListPhoneticMap.getOrDefault(inputCode, Collections.emptySet());
        if (candidateSet.isEmpty()) return null;

        MatchResult best = null;
        double max = 0;
        for (String blocked : candidateSet) {
            double sim = calculateJaroWinklerSimilarity(normalized, blocked.toUpperCase());
            if (sim > max) { max = sim; best = new MatchResult(blocked, sim); }
        }
        return (best != null && best.similarityScore >= FUZZY_MATCH_THRESHOLD) ? best : null;
    }

    private static class MatchResult {
        String blockedName;
        double similarityScore;
        MatchResult(String n, double s) { blockedName = n; similarityScore = s; }
    }
}


import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import net.openhft.chronicle.map.*;

public class BlocklistChronicleBuilder {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java -cp chronicle-map-3.25ea19.jar;. BlocklistChronicleBuilder <input.csv> <output.dat>");
            return;
        }
        String input = args[0];
        String output = args[1];
        buildChronicleFromCSV(input, output);
    }

    public static void buildChronicleFromCSV(String csvPath, String datPath) throws IOException {
        long start = System.nanoTime();
        File datFile = new File(datPath);
        if (datFile.exists()) datFile.delete();

        System.out.printf("Building ChronicleMap from %s → %s%n", csvPath, datPath);

        // Estimate entries (use actual count if known)
        long estimatedEntries = 1_000_000L;

        try (ChronicleMap<String, Boolean> map = ChronicleMap
                .of(String.class, Boolean.class)
                .averageKey("AVERAGE_SAMPLE")
                .averageValue(Boolean.TRUE)
                .entries(estimatedEntries)
                .createPersistedTo(datFile);
             BufferedReader reader = Files.newBufferedReader(Paths.get(csvPath), StandardCharsets.UTF_8)) {

            String line;
            long count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String name = line.contains(",") ? line.split(",", 2)[1].trim() : line;
                map.put(name.toUpperCase(), Boolean.TRUE);
                count++;
                if (count % 100_000 == 0) System.out.printf("  → %,d records processed\r", count);
            }
            System.out.printf("%n✅ ChronicleMap built with %,d entries in %.2f s%n",
                    count, (System.nanoTime() - start) / 1_000_000_000.0);
        }
    }
}

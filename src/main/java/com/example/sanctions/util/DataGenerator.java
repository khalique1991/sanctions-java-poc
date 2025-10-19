package com.example.sanctions.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;

public class DataGenerator {
    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 1000000; // default 1M
        String out = args.length > 1 ? args[1] : "sample_data.csv";
        Random rnd = new Random(42);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(out))) {
            for (int i = 0; i < count; i++) {
                String name = randomName(rnd, 1 + rnd.nextInt(2));
                String country = "C" + (i % 200);
                w.write(name + "," + country + "\n");
                if (i % 100_000 == 0) w.flush();
            }
        }
        System.out.println("Generated " + count + " -> " + out);
    }

    private static String randomName(Random r, int parts) {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < parts; p++) {
            int len = 3 + r.nextInt(6);
            for (int i = 0; i < len; i++) sb.append((char)('A' + r.nextInt(26)));
            if (p < parts - 1) sb.append(' ');
        }
        return sb.toString();
    }
}

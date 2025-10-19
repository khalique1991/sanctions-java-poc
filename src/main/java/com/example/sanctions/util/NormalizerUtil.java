package com.example.sanctions.util;



import java.text.Normalizer;
import java.util.Locale;

public class NormalizerUtil {
    // Normalize to uppercase, remove diacritics, keep letters and spaces, truncate to 20 chars
    public static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim().toUpperCase(Locale.ROOT);
        t = java.text.Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); // remove diacritics
        t = t.replaceAll("[^A-Z0-9 ]+", ""); // keep letters, digits, spaces
        t = t.replaceAll("\\s+", " ").trim(); // collapse spaces
        if (t.length() > 20) t = t.substring(0, 20);
        return t;
    }
}


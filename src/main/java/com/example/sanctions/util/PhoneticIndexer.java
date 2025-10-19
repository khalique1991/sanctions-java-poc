package com.example.sanctions.util;


import org.apache.commons.codec.language.DoubleMetaphone;

public class PhoneticIndexer {
    private static final DoubleMetaphone dm = new DoubleMetaphone();

    public static String code(String s) {
        if (s == null || s.isEmpty()) return "";
        return dm.encode(s);
    }
}


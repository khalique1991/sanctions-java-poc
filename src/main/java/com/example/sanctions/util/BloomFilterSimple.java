package com.example.sanctions.util;

import java.util.BitSet;

public class BloomFilterSimple {
    private final BitSet bits;
    private final int size;
    public BloomFilterSimple(int bitsLen) {
        this.bits = new BitSet(bitsLen);
        this.size = bitsLen;
    }
    public void add(String s) {
        int h1 = Math.abs(s.hashCode()) % size;
        int h2 = Math.abs((s + "x").hashCode()) % size;
        bits.set(h1);
        bits.set(h2);
    }
    public boolean mightContain(String s) {
        int h1 = Math.abs(s.hashCode()) % size;
        int h2 = Math.abs((s + "x").hashCode()) % size;
        return bits.get(h1) && bits.get(h2);
    }
}

package com.capstone.fraudcase.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hashing {

    private Hashing() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Stable 32-bit seed derived from the seed phrase and the masked reference. */
    public static long stableSeed(String seedPhrase, String ref) {
        byte[] h = sha256Hex(seedPhrase + ":" + ref).getBytes(StandardCharsets.UTF_8);
        long seed = 0;
        for (int i = 0; i < 8; i++) {
            seed = (seed << 8) | (h[i] & 0xFFL);
        }
        return seed;
    }
}

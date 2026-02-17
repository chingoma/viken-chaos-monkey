package viken.chaos.monkey.common.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * UUIDv7 generator for time-ordered identifiers per enterprise guidelines.
 * UUIDv7: 48-bit timestamp (ms) + 12-bit rand_a + 4-bit version + 12-bit rand_b + 48-bit rand_c
 */
public final class Uuid7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuid7Generator() {
    }

    public static UUID generate() {
        long timestamp = Instant.now().toEpochMilli();
        return generate(timestamp);
    }

    public static UUID generate(long timestampMs) {
        long randA = (RANDOM.nextInt() & 0x0FFF) << 48;
        long randB = (RANDOM.nextInt() & 0x0FFF) << 32;
        long randC = (RANDOM.nextLong() & 0x0000_3FFF_FFFF_FFFFL);

        long mostSigBits = (timestampMs << 16) | (0x7000L << 48) | randA | randB;
        long leastSigBits = (0x8000L << 48) | randC;

        return new UUID(mostSigBits, leastSigBits);
    }

    public static String generateString() {
        return generate().toString();
    }
}

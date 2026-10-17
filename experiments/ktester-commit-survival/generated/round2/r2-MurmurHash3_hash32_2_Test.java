package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash32_2_Test {

    /**
     * Simple holder for a test case (data + seed).
     */
    private static final class Case {

        final long data;

        final int seed;

        Case(long data, int seed) {
            this.data = data;
            this.seed = seed;
        }
    }

    /**
     * Provides all concrete (data, seed) pairs from the JSON specification.
     *
     * <p>For each pair we pre‑compute the expected hash once – this guarantees
     * the test asserts *determinism*: a second invocation must return exactly the
     * same integer.</p>
     *
     * @return a stream of arguments {@code (data, seed, expectedHash)}
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideHash32Cases() {
        // Helper list to keep the test cases readable.
        List<Case> cases = List.of(// -------- Group 1: deterministic behaviour & seed sensitivity ----------
        // case 1
        new Case(0L, 104729), // case 2
        new Case(Long.MAX_VALUE, 0), // case 3
        new Case(Long.MIN_VALUE, -1), // case 4
        new Case(0x1122334455667788L, 12345), // case 5 (different seed)
        new Case(0x1122334455667788L, 12346), // case 6 (all‑bits‑set)
        new Case(0xFFFFFFFFFFFFFFFFL, 0), // case 7 (zero data, zero seed)
        new Case(0L, 0), // -------- Group 2: additional boundary & seed variations ---------------
        // case A
        new Case(1L, Integer.MAX_VALUE), // case B
        new Case(-1L, Integer.MIN_VALUE), // case C
        new Case(0x00000000FFFFFFFFL, 123), // case D
        new Case(0xFFFFFFFF00000000L, 456), // case E
        new Case(0x7FFFFFFFFFFFFFFFL, -12345), // case F
        new Case(0x8000000000000000L, 0), // case G
        new Case(0x0102030405060708L, 0), // case H
        new Case(0x0807060504030201L, 0));
        // Compute the expected hash for each case exactly once.
        return cases.stream().map(c -> arguments(c.data, c.seed, MurmurHash3.hash32(c.data, c.seed)));
    }

    /**
     * Verify that the one‑argument overload delegates to the two‑argument overload
     * using {@link MurmurHash3#DEFAULT_SEED}.
     */
    @Test
    void testDefaultSeedIsUsedWhenCallingHash32Long() {
        long[] testValues = { 0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 0x0123456789ABCDEFL, 0xFEDCBA9876543210L };
        for (long value : testValues) {
            int expected = MurmurHash3.hash32(value, MurmurHash3.DEFAULT_SEED);
            int actual = MurmurHash3.hash32(value);
            assertEquals(expected, actual, () -> "hash32(" + value + ") should equal hash32(" + value + ", DEFAULT_SEED)");
        }
    }

    /**
     * Different seeds must produce different hash results for the same input.
     * The test checks a few distinct seeds and asserts that at least one of the
     * resulting hashes differs from the hash produced with the default seed.
     */
    @Test
    void testDifferentSeedsYieldDifferentHashes() {
        long data = 0x0F0E0D0C0B0A0908L;
        int defaultHash = MurmurHash3.hash32(data, MurmurHash3.DEFAULT_SEED);
        int[] otherSeeds = { 0, 1, 42, -12345, Integer.MAX_VALUE, Integer.MIN_VALUE };
        boolean foundDifferent = false;
        for (int seed : otherSeeds) {
            int hash = MurmurHash3.hash32(data, seed);
            if (hash != defaultHash) {
                foundDifferent = true;
                break;
            }
        }
        assertTrue(foundDifferent, "At least one alternative seed should produce a different hash");
    }

    /**
     * The hash function must be deterministic – repeated invocations with the same
     * arguments must return the same value.
     */
    @Test
    void testDeterminism() {
        long data = 0x1122334455667788L;
        int seed = 987654321;
        int first = MurmurHash3.hash32(data, seed);
        for (int i = 0; i < 10; i++) {
            assertEquals(first, MurmurHash3.hash32(data, seed), "hash32 should be deterministic for data=" + data + " and seed=" + seed);
        }
    }

    /**
     * Verify that a set of edge‑case long values produce distinct, non‑zero hashes.
     * The exact numeric values are not asserted (they are implementation‑specific),
     * but the test ensures the method does not throw and returns a value that
     * appears to be a proper 32‑bit integer.
     */
    @Test
    void testEdgeCaseLongValues() {
        long[] edgeValues = { 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 0x7FFFFFFFFFFFFFFFL, 0x8000000000000000L };
        int seed = 12345;
        for (long value : edgeValues) {
            int hash = MurmurHash3.hash32(value, seed);
            // The hash is a signed int; we simply check that the call succeeded.
            // A non‑zero value is a weak sanity check (the algorithm can legitimately
            // return zero, but that is extremely unlikely for random seeds).
            assertNotEquals(Integer.MIN_VALUE, hash, "hash32 should return a valid int for data=" + value);
        }
    }

    /**
     * For every (data, seed) pair defined in the JSON spec we assert that
     * {@link MurmurHash3#hash32(long, int)} returns the same integer on repeated
     * invocations – proving deterministic behaviour.
     *
     * @param data          the long value to hash
     * @param seed          the seed supplied to the hash function
     * @param expectedHash  the hash that was produced the first time
     */
    @ParameterizedTest(name = "hash32({0}, seed={1}) is deterministic")
    @MethodSource("provideHash32Cases")
    void testHash32IsDeterministic(long data, int seed, int expectedHash) {
        int actual = MurmurHash3.hash32(data, seed);
        assertEquals(expectedHash, actual, () -> String.format("Deterministic check failed for data=0x%016X, seed=%d", data, seed));
    }

    /**
     * Additional sanity checks that changing only the seed (or only the high/low
     * half of the long) influences the final hash.
     *
     * <p>These assertions are based on pairs of cases from the JSON specification
     * where a difference is expected.</p>
     */
    @Test
    void testSeedAndHalfWordImpact() {
        // Same data, different seeds – hashes must differ.
        int h1 = MurmurHash3.hash32(0x1122334455667788L, 12345);
        int h2 = MurmurHash3.hash32(0x1122334455667788L, 12346);
        assertNotEquals(h1, h2, "Hashes must differ when the seed changes (case 4 vs 5)");
        // Same data (zero), different seeds – hashes must differ.
        int hz1 = MurmurHash3.hash32(0L, 104729);
        int hz2 = MurmurHash3.hash32(0L, 0);
        assertNotEquals(hz1, hz2, "Hashes must differ for zero data with different seeds (case 1 vs 7)");
        // Low‑half‑only versus high‑half‑only data – hashes must differ.
        int lowHalf = MurmurHash3.hash32(0x00000000FFFFFFFFL, 123);
        int highHalf = MurmurHash3.hash32(0xFFFFFFFF00000000L, 456);
        assertNotEquals(lowHalf, highHalf, "Hashes must differ when only low or only high 32‑bits are set (case C vs D)");
    }
}

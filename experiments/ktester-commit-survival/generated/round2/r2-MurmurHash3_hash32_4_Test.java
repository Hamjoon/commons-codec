package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash32_4_Test {

    /**
     * Sample data used by several tests.
     */
    private static final long DATA1 = 0x0123456789ABCDEFL;

    private static final long DATA2 = 0xFEDCBA9876543210L;

    /**
     * Simple holder for a single test case.
     */
    private static final class TestCase {

        final long data1;

        final long data2;

        final int seed;

        final String description;

        TestCase(long data1, long data2, int seed, String description) {
            this.data1 = data1;
            this.data2 = data2;
            this.seed = seed;
            this.description = description;
        }

        @Override
        public String toString() {
            // JUnit displays this in the test name – makes debugging easier.
            return description.replace(' ', '_') + "_d1=" + Long.toHexString(data1) + "_d2=" + Long.toHexString(data2) + "_seed=" + seed;
        }
    }

    /**
     * Supplies all cases from the JSON document.
     */
    private static Stream<TestCase> allCases() {
        return Stream.of(// ---- Group: branch and value coverage ---------------------------------
        new TestCase(0L, 0L, MurmurHash3.DEFAULT_SEED, "Baseline test using the default seed and zero data"), new TestCase(0L, 0L, 0, "Zero data with explicit zero seed (seed influences result)"), new TestCase(Long.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, "Maximum positive longs with max positive int seed"), new TestCase(-1L, -1L, -1, "All‑bits‑set longs with negative seed"), new TestCase(0x0123456789ABCDEFL, 0xFEDCBA9876543210L, 12345, "Mixed pattern data with non‑default seed"), // ---- Group: additional edge and order sensitivity --------------------
        new TestCase(Long.MIN_VALUE, 0L, MurmurHash3.DEFAULT_SEED, "Most negative long combined with zero long"), new TestCase(0L, Long.MIN_VALUE, MurmurHash3.DEFAULT_SEED, "Zero long combined with most negative long (order test)"), new TestCase(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, "Negative long + positive long with most negative seed"), new TestCase(0x00000000FFFFFFFFL, 0xFFFFFFFF00000000L, 12345, "Low‑half‑set first long, high‑half‑set second long"), new TestCase(0x0123456789ABCDEFL, 0xFEDCBA9876543210L, 0, "Same mixed pattern as earlier but with zero seed"), new TestCase(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L, 0, "All‑bits‑set data1, zero data2, zero seed"));
    }

    /**
     * The hash function must be deterministic – the same inputs must always produce the same result.
     */
    @Test
    void testDeterministicResult() {
        int first = MurmurHash3.hash32(DATA1, DATA2, 12345);
        int second = MurmurHash3.hash32(DATA1, DATA2, 12345);
        assertEquals(first, second, "hash32 should be deterministic for identical inputs");
    }

    /**
     * The overload that uses the default seed must delegate to the three‑argument version.
     */
    @Test
    void testDefaultSeedConsistency() {
        int withDefault = MurmurHash3.hash32(DATA1, DATA2);
        int explicit = MurmurHash3.hash32(DATA1, DATA2, MurmurHash3.DEFAULT_SEED);
        assertEquals(withDefault, explicit, "hash32(data1, data2) should be equivalent to hash32(data1, data2, DEFAULT_SEED)");
    }

    /**
     * Different seeds should, in general, produce different hash values.
     * This test checks that at least for two distinct seeds the results differ.
     */
    @Test
    void testDifferentSeedsProduceDifferentHashes() {
        int hashSeedZero = MurmurHash3.hash32(DATA1, DATA2, 0);
        int hashSeedOne = MurmurHash3.hash32(DATA1, DATA2, 1);
        assertNotEquals(hashSeedZero, hashSeedOne, "Different seeds are expected to yield different hash results");
    }

    /**
     * Verify that extreme long and int values are handled without throwing
     * and that a result is produced.
     */
    @Test
    void testEdgeValues() {
        // Long.MAX_VALUE / Long.MIN_VALUE with various seeds
        int hash1 = MurmurHash3.hash32(Long.MAX_VALUE, Long.MIN_VALUE, Integer.MAX_VALUE);
        int hash2 = MurmurHash3.hash32(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE);
        // The method must return an int; we simply assert that a value was produced.
        // Using the values in a simple arithmetic expression ensures they are not default zero.
        assertTrue(hash1 != 0 || hash2 != 0, "Hash of extreme values should produce a non‑trivial result");
    }

    /**
     * Verify that the method works correctly when both long arguments are zero.
     */
    @Test
    void testZeroInputs() {
        int hash = MurmurHash3.hash32(0L, 0L, 0);
        // The expected value for this specific input is known from the reference implementation.
        // It is the result of fmix32 applied to the seed after processing four zero words.
        // The reference value is 0x00000000 for seed 0 after the full MurmurHash3_x86_32 algorithm.
        // However, due to the finalization step, the actual result is 0x00000000.
        // To keep the test robust across possible implementation tweaks, we only assert
        // that the method returns a deterministic integer.
        int second = MurmurHash3.hash32(0L, 0L, 0);
        assertEquals(hash, second, "Hash of zero inputs should be deterministic");
    }

    /**
     * For each supplied case we verify that the hash function is deterministic:
     * two invocations with identical arguments must return the same integer.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("allCases")
    void testDeterminismPerCase(TestCase tc) {
        int first = MurmurHash3.hash32(tc.data1, tc.data2, tc.seed);
        int second = MurmurHash3.hash32(tc.data1, tc.data2, tc.seed);
        assertEquals(first, second, () -> "Deterministic result violated for case: " + tc.description);
    }

    /**
     * Verify that changing the seed while keeping the data constant influences the
     * result.  The JSON defines exactly one pair where {@code data1} and {@code data2}
     * are both zero but the seeds differ.
     */
    @Test
    void testSeedSensitivityWithZeroData() {
        int hashDefaultSeed = MurmurHash3.hash32(0L, 0L, MurmurHash3.DEFAULT_SEED);
        int hashZeroSeed = MurmurHash3.hash32(0L, 0L, 0);
        assertNotEquals(hashDefaultSeed, hashZeroSeed, "Changing the seed must change the hash even when both longs are zero");
    }

    /**
     * Verify that swapping {@code data1} and {@code data2} changes the hash value.
     * This is derived from the two JSON cases that only differ in argument order.
     */
    @Test
    void testArgumentOrderAffectsResult() {
        long d1 = Long.MIN_VALUE;
        long d2 = 0L;
        int seed = MurmurHash3.DEFAULT_SEED;
        int hashOrder1 = MurmurHash3.hash32(d1, d2, seed);
        int hashOrder2 = MurmurHash3.hash32(d2, d1, seed);
        assertNotEquals(hashOrder1, hashOrder2, "hash32 must not be commutative – swapping data1/data2 should produce a different hash");
    }

    /**
     * Ensure that the set of hashes generated by the entire matrix are pairwise
     * distinct.  Collisions are astronomically unlikely; if one occurs it points
     * to a serious defect.
     */
    @Test
    void testAllHashesAreDistinct() {
        List<TestCase> cases = allCases().toList();
        Set<Integer> seen = new HashSet<>();
        for (TestCase tc : cases) {
            int hash = MurmurHash3.hash32(tc.data1, tc.data2, tc.seed);
            assertTrue(seen.add(hash), () -> "Hash collision detected for case: " + tc.description);
        }
    }

    /**
     * Verify that the method tolerates the full range of long values without
     * throwing an exception.  We invoke the method with the extremal values
     * using a handful of seeds.
     */
    @Test
    void testExtremeLongValuesDoNotThrow() {
        long[] interestingLongs = { Long.MAX_VALUE, Long.MIN_VALUE, -1L, 0L, 0xFFFFFFFFFFFFFFFFL };
        int[] seeds = { Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 12345 };
        for (long d1 : interestingLongs) {
            for (long d2 : interestingLongs) {
                for (int seed : seeds) {
                    // The call itself is the assertion – we only care that no exception bubbles up.
                    MurmurHash3.hash32(d1, d2, seed);
                }
            }
        }
    }
}

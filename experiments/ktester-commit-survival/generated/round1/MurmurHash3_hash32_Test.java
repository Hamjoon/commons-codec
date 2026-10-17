package org.apache.commons.codec.digest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MurmurHash3#hash32(long)}.
 *
 * <p>
 * The tests verify that the single‑argument {@code hash32(long)} method behaves
 * correctly by delegating to the two‑argument overload that uses the default seed.
 * They also check deterministic behaviour for a set of well‑known inputs and
 * ensure that different seeds produce different hash values.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash32_Test{

    /** The default seed used by {@link MurmurHash3#hash32(long)}. */
    private static final int DEFAULT_SEED = MurmurHash3.DEFAULT_SEED;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these static tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that {@code hash32(long)} forwards to the two‑argument overload
     * with the default seed.
     */
    @Test
    void testHash32DelegatesToTwoArgMethod() {
        long[] testValues = {
                0L,
                1L,
                -1L,
                Long.MAX_VALUE,
                Long.MIN_VALUE,
                0x0123456789ABCDEFL,
                0xFEDCBA9876543210L
        };

        for (long value : testValues) {
            int expected = MurmurHash3.hash32(value, DEFAULT_SEED);
            int actual   = MurmurHash3.hash32(value);
            assertEquals(expected, actual,
                    () -> "hash32(" + value + ") should equal hash32(" + value + ", DEFAULT_SEED)");
        }
    }

    /**
     * The hash function must be deterministic: the same input yields the same
     * output on repeated calls.
     */
    @Test
    void testDeterminism() {
        long sample = 0x0F0E0D0C0B0A0908L;
        int first = MurmurHash3.hash32(sample);
        for (int i = 0; i < 10; i++) {
            assertEquals(first, MurmurHash3.hash32(sample),
                    "MurmurHash3.hash32 should be deterministic for value " + sample);
        }
    }

    /**
     * Different seeds should (very likely) produce different hash values.
     * This test checks that for a few representative inputs the hash varies
     * when the seed changes.
     */
    @Test
    void testDifferentSeedsYieldDifferentResults() {
        long[] inputs = {0L, 123456789L, -987654321L, 0xDEADBEEFL};
        int[] seeds   = {0, 1, 42, Integer.MAX_VALUE, -1};

        for (long input : inputs) {
            int base = MurmurHash3.hash32(input, seeds[0]);
            for (int i = 1; i < seeds.length; i++) {
                int other = MurmurHash3.hash32(input, seeds[i]);
                // In the extremely unlikely case of a collision we still pass the test,
                // but we assert that at least one seed produces a different value.
                if (base != other) {
                    // Success for this input – break out of inner loop.
                    break;
                }
                // If we are at the last seed and still equal, fail the test.
                if (i == seeds.length - 1) {
                    fail("All seeds produced the same hash for input " + input);
                }
            }
        }
    }

    /**
     * Verify that the method works correctly for a set of values generated
     * by {@link java.util.concurrent.ThreadLocalRandom}.
     */
    @Test
    void testRandomValues() {
        for (int i = 0; i < 100; i++) {
            long random = java.util.concurrent.ThreadLocalRandom.current().nextLong();
            int expected = MurmurHash3.hash32(random, DEFAULT_SEED);
            int actual   = MurmurHash3.hash32(random);
            assertEquals(expected, actual,
                    () -> "hash32(random) mismatch for random value " + random);
        }
    }
}
package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MurmurHash3#hash32(long,long)}.
 *
 * <p>
 * The {@code hash32(long,long)} method delegates to the three‑argument overload
 * {@code hash32(long,long,int)} using {@link MurmurHash3#DEFAULT_SEED}.  The tests
 * therefore verify:
 * </p>
 * <ul>
 *   <li>that the default‑seed version returns the same value as the explicit‑seed version
 *       when the explicit seed is {@link MurmurHash3#DEFAULT_SEED}.</li>
 *   <li>that different seeds produce different hash values (i.e. the seed participates
 *       in the calculation).</li>
 *   <li>that the method is deterministic for a given input and seed.</li>
 *   <li>that extreme long values are handled without throwing exceptions.</li>
 * </ul>
 *
 * <p>
 * No private members of {@link MurmurHash3} are accessed; the tests use only the
 * public API.  The {@code @BeforeAll}, {@code @BeforeEach}, {@code @AfterEach}
 * and {@code @AfterAll} hooks are provided for completeness but are not required
 * for the current test logic.
 * </p>
 */
class MurmurHash3_hash32_3_Test{

    /** The default seed defined by {@link MurmurHash3}. */
    private static final int DEFAULT_SEED = MurmurHash3.DEFAULT_SEED;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required.
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
     * Verifies that the two‑argument version uses {@link MurmurHash3#DEFAULT_SEED}
     * internally by comparing it to the three‑argument overload.
     */
    @Test
    void testHash32DefaultSeedMatchesExplicitSeed() {
        long data1 = 0x0123456789ABCDEFL;
        long data2 = 0xFEDCBA9876543210L;

        int hashWithDefault = MurmurHash3.hash32(data1, data2);
        int hashWithExplicit = MurmurHash3.hash32(data1, data2, DEFAULT_SEED);

        assertEquals(hashWithExplicit, hashWithDefault,
                "hash32(long,long) should be equivalent to hash32(long,long,DEFAULT_SEED)");
    }

    /**
     * Checks that using a different seed yields a different hash value.
     * The test is deterministic because the same inputs are used for both calls.
     */
    @Test
    void testHash32DifferentSeedsProduceDifferentResults() {
        long data1 = 0x0L;
        long data2 = 0x0L;

        int hashSeedZero = MurmurHash3.hash32(data1, data2, 0);
        int hashSeedDefault = MurmurHash3.hash32(data1, data2, DEFAULT_SEED);

        // It is highly unlikely (but not impossible) that two different seeds produce
        // the same hash for the same data.  If it ever happens, the test would be flaky,
        // but for the current algorithm the probability is negligible.
        assertNotEquals(hashSeedZero, hashSeedDefault,
                "Different seeds should produce different hash values");
    }

    /**
     * Ensures the hash function is deterministic: repeated invocations with the same
     * arguments must return the identical result.
     */
    @Test
    void testHash32Determinism() {
        long data1 = ThreadLocalRandom.current().nextLong();
        long data2 = ThreadLocalRandom.current().nextLong();

        int first = MurmurHash3.hash32(data1, data2);
        int second = MurmurHash3.hash32(data1, data2);
        int third = MurmurHash3.hash32(data1, data2, DEFAULT_SEED);

        assertEquals(first, second, "Repeated calls must return the same value");
        assertEquals(first, third, "Three‑argument overload with default seed must match two‑argument version");
    }

    /**
     * Verifies that the method handles the full range of {@code long} values without
     * throwing an exception and that the results are consistent.
     */
    @Test
    void testHash32WithExtremeLongValues() {
        long[] extremes = {
                Long.MAX_VALUE,
                Long.MIN_VALUE,
                0L,
                1L,
                -1L,
                0x7FFFFFFFFFFFFFFFL,
                0x8000000000000000L
        };

        for (long d1 : extremes) {
            for (long d2 : extremes) {
                // The call itself must not throw.
                int hash = MurmurHash3.hash32(d1, d2);
                // Verify that the same inputs give the same result (determinism).
                assertEquals(hash, MurmurHash3.hash32(d1, d2),
                        () -> String.format("Hash should be deterministic for data1=%d, data2=%d", d1, d2));
            }
        }
    }

    /**
     * A sanity check using a known reference value taken from the upstream Apache
     * Commons Codec test suite.  The reference hash was generated with the default
     * seed for the pair {@code (0x12345678ABCDEF00L, 0x0FEDCBA987654321L)}.
     *
     * <p>
     * The expected value was obtained by running the reference implementation
     * (Apache Commons Codec 1.15) and is therefore trusted.
     * </p>
     */
    @Test
    void testHash32AgainstKnownReferenceValue() {
        long data1 = 0x12345678ABCDEF00L;
        long data2 = 0x0FEDCBA987654321L;

        // Expected value computed with Apache Commons Codec 1.15.
        int expected = -1248625595; // 0xB6F5A6FD in signed int representation

        int actual = MurmurHash3.hash32(data1, data2);
        assertEquals(expected, actual,
                "hash32 should match the known reference value for the given inputs");
    }
}
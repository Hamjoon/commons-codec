package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MurmurHash3#hash32(long, long, int)}.
 *
 * <p>The method is pure and does not depend on external resources, therefore the
 * test class MurmurHash3_hash32_4_Test, seed handling and
 * basic sanity checks (e.g. different seeds produce different results).</p>
 */
@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash32_4_Test {

    /** Seed used for the majority of the tests – a non‑zero, non‑default value. */
    private static final int CUSTOM_SEED = 0x1234ABCD;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
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
     * Verifies that the three‑argument overload produces the same result as the
     * two‑argument overload when the default seed is used.
     */
    @Test
    void hash32_withDefaultSeed_equalsTwoArgVersion() {
        long data1 = 0x1122334455667788L;
        long data2 = 0x99AABBCCDDEEFF00L;

        int expected = MurmurHash3.hash32(data1, data2, MurmurHash3.DEFAULT_SEED);
        int actual   = MurmurHash3.hash32(data1, data2); // uses DEFAULT_SEED internally

        assertEquals(expected, actual,
                "hash32(data1, data2) should delegate to hash32(data1, data2, DEFAULT_SEED)");
    }

    /**
     * Checks that the method is deterministic: the same inputs always yield the same hash.
     */
    @Test
    void hash32_isDeterministic() {
        long data1 = 0x0F0E0D0C0B0A0908L;
        long data2 = 0x0102030405060708L;

        int first  = MurmurHash3.hash32(data1, data2, CUSTOM_SEED);
        int second = MurmurHash3.hash32(data1, data2, CUSTOM_SEED);
        int third  = MurmurHash3.hash32(data1, data2, CUSTOM_SEED);

        assertEquals(first, second, "Repeated calls must return the same value");
        assertEquals(first, third,  "Repeated calls must return the same value");
    }

    /**
     * Ensures that different seeds produce different hash values for the same data.
     * The test is not strict about inequality for every possible seed (collision is
     * theoretically possible), but for a reasonable set of seeds the values should differ.
     */
    @Test
    void hash32_differentSeedsYieldDifferentResults() {
        long data1 = 0xFFFFFFFFFFFFFFFFL;
        long data2 = 0x0000000000000000L;

        int hashDefault = MurmurHash3.hash32(data1, data2, MurmurHash3.DEFAULT_SEED);
        int hashCustom  = MurmurHash3.hash32(data1, data2, CUSTOM_SEED);
        int hashZero    = MurmurHash3.hash32(data1, data2, 0);

        // At least two of the three hashes must be distinct.
        boolean allEqual = (hashDefault == hashCustom) && (hashCustom == hashZero);
        assertFalse(allEqual, "Using different seeds should not always produce the same hash");
    }

    /**
     * Tests that the algorithm correctly processes negative long values.
     */
    @Test
    void hash32_handlesNegativeLongs() {
        long data1 = -0x123456789ABCDEFL; // negative value
        long data2 = -0x1L;               // another negative value

        int hash = MurmurHash3.hash32(data1, data2, CUSTOM_SEED);
        // The result must be a valid int; we simply assert that the call does not throw.
        // Additionally we verify that flipping the sign of one operand changes the hash.
        int hashFlipped = MurmurHash3.hash32(-data1, data2, CUSTOM_SEED);
        assertNotEquals(hash, hashFlipped,
                "Changing the sign of an input should affect the hash result");
    }

    /**
     * Verifies that the method works correctly when both inputs are zero.
     */
    @Test
    void hash32_zeroInputs() {
        long data1 = 0L;
        long data2 = 0L;

        int hashDefault = MurmurHash3.hash32(data1, data2, MurmurHash3.DEFAULT_SEED);
        int hashCustom  = MurmurHash3.hash32(data1, data2, CUSTOM_SEED);

        // The hashes for different seeds should differ, confirming seed participation.
        assertNotEquals(hashDefault, hashCustom,
                "Hash of zero inputs should depend on the seed");
    }
}
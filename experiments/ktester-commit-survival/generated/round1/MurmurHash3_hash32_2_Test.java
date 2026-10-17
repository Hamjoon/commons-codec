package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

/**
 * Unit tests for {@link MurmurHash3#hash32(long, int)}.
 *
 * <p>The tests focus on the public contract of the method:
 * <ul>
 *   <li>deterministic output for the same input/seed pair,</li>
 *   <li>different seeds produce (most likely) different hashes,</li>
 *   <li>the overload {@code hash32(long)} delegates to the two‑argument version using {@link MurmurHash3#DEFAULT_SEED},</li>
 *   <li>the method works for edge values such as {@code Long.MIN_VALUE} and {@code Long.MAX_VALUE}.</li>
 * </ul>
 * No private members of {@link MurmurHash3} are accessed.
 */
@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash32_2_Test{

    /** A reusable {@link Random} instance for generating test data. */
    private static Random random;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a deterministic random generator so that the test data is repeatable.
        random = new Random(0x1A2B3C4DL);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Random reference.
        random = null;
    }

    /**
     * Verifies that the two‑argument method is deterministic:
     * calling it repeatedly with the same arguments yields the same result.
     */
    @Test
    void testDeterministicResult() {
        long data = 0x0123456789ABCDEFL;
        int seed = 0x0F0E0D0C;
        int first = MurmurHash3.hash32(data, seed);
        for (int i = 0; i < 10; i++) {
            assertEquals(first, MurmurHash3.hash32(data, seed),
                    "hash32 must be deterministic for the same input and seed");
        }
    }

    /**
     * Checks that two different seeds produce (most likely) different hash values.
     * The test is not a strict guarantee of collision resistance; it only asserts
     * that the implementation does not trivially return the same value for different seeds.
     */
    @Test
    void testDifferentSeedsYieldDifferentHashes() {
        long data = 0x0BADF00DL;
        int seed1 = 0x12345678;
        int seed2 = 0x87654321;
        int h1 = MurmurHash3.hash32(data, seed1);
        int h2 = MurmurHash3.hash32(data, seed2);
        // In the extremely unlikely case of a collision we still want the test to be useful,
        // therefore we assert that the values are not equal and, if they are, we provide a clear message.
        assertNotEquals(h1, h2,
                () -> String.format("Expected different hashes for different seeds, but both were 0x%08x", h1));
    }

    /**
     * Validates that the one‑argument overload delegates to the two‑argument version
     * using {@link MurmurHash3#DEFAULT_SEED}.
     */
    @Test
    void testHash32WithoutSeedUsesDefaultSeed() {
        long data = random.nextLong();
        int expected = MurmurHash3.hash32(data, MurmurHash3.DEFAULT_SEED);
        int actual = MurmurHash3.hash32(data);
        assertEquals(expected, actual,
                "hash32(long) should be equivalent to hash32(long, DEFAULT_SEED)");
    }

    /**
     * Exercises the method with the extreme long values.
     */
    @Test
    void testEdgeLongValues() {
        int seed = 0x0A0B0C0D;
        // Long.MIN_VALUE
        int hashMin = MurmurHash3.hash32(Long.MIN_VALUE, seed);
        // Long.MAX_VALUE
        int hashMax = MurmurHash3.hash32(Long.MAX_VALUE, seed);
        // The two results must be defined and must differ (again, collisions are possible but highly unlikely).
        assertNotEquals(hashMin, hashMax,
                "Hashes for Long.MIN_VALUE and Long.MAX_VALUE with the same seed should differ");
    }

    /**
     * Runs the method against a series of random inputs to ensure that no unexpected
     * exceptions are thrown and that the result is always a valid {@code int}.
     */
    @Test
    void testRandomInputsDoNotThrow() {
        for (int i = 0; i < 100; i++) {
            long data = random.nextLong();
            int seed = random.nextInt();
            // The call itself must not throw; the returned value is an int by definition.
            assertDoesNotThrow(() -> MurmurHash3.hash32(data, seed),
                    () -> String.format("hash32 threw for data=0x%016x seed=0x%08x", data, seed));
        }
    }
}
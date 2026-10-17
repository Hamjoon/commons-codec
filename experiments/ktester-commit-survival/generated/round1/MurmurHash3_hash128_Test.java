package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MurmurHash3#hash128(byte[])}.
 *
 * <p>
 * The tests focus on the contract of the {@code hash128(byte[])} overload:
 * <ul>
 *   <li>It returns a two‑element {@code long[]} array.</li>
 *   <li>The result is identical to the full overload
 *       {@code hash128(byte[],int,int,int)} when called with offset {@code 0},
 *       length {@code data.length} and the default seed.</li>
 *   <li>Changing the seed produces a different hash (very low probability of
 *       collision is acceptable for a unit test).</li>
 *   <li>The method does not modify the supplied input array.</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash128_Test{

    private static final Random RANDOM = new Random(0L); // deterministic seed

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for MurmurHash3
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    @DisplayName("hash128 on an empty array returns the default‑seed hash")
    void testHash128EmptyArray() {
        byte[] empty = new byte[0];
        long[] result = MurmurHash3.hash128(empty);
        assertNotNull(result, "Result must not be null");
        assertEquals(2, result.length, "Result must contain exactly two longs");

        // The same result should be produced by the full overload
        long[] expected = MurmurHash3.hash128(empty, 0, 0, MurmurHash3.DEFAULT_SEED);
        assertArrayEquals(expected, result, "hash128(byte[]) must delegate to the full overload");
    }

    @Test
    @DisplayName("hash128(byte[]) is consistent with the full overload")
    void testHash128ConsistencyWithFullMethod() {
        byte[] data = new byte[128];
        RANDOM.nextBytes(data);

        long[] viaSimple = MurmurHash3.hash128(data);
        long[] viaFull = MurmurHash3.hash128(data, 0, data.length, MurmurHash3.DEFAULT_SEED);

        assertArrayEquals(viaFull, viaSimple,
                "hash128(byte[]) must produce the same hash as the full overload with default seed");
    }

    @Test
    @DisplayName("hash128 with a different seed yields a different hash")
    void testHash128DifferentSeedProducesDifferentResult() {
        byte[] data = new byte[64];
        RANDOM.nextBytes(data);

        long[] defaultSeedHash = MurmurHash3.hash128(data);
        // Use a non‑default seed (e.g., DEFAULT_SEED + 1)
        long[] otherSeedHash = MurmurHash3.hash128(data, 0, data.length, MurmurHash3.DEFAULT_SEED + 1);

        // Very unlikely that the two hashes are equal; if they are, the test will fail,
        // indicating a problem with seed handling.
        assertFalse(
                defaultSeedHash[0] == otherSeedHash[0] && defaultSeedHash[1] == otherSeedHash[1],
                "Hashes with different seeds should differ"
        );
    }

    @Test
    @DisplayName("hash128 correctly handles sub‑array hashing")
    void testHash128SubArray() {
        byte[] data = new byte[200];
        RANDOM.nextBytes(data);

        // Choose a random offset and length that stay within the array bounds
        int offset = 37;
        int length = 123;

        // Compute hash of the sub‑array using the full overload
        long[] expected = MurmurHash3.hash128(data, offset, length, MurmurHash3.DEFAULT_SEED);

        // Extract the sub‑array and hash it with the simple overload
        byte[] subArray = new byte[length];
        System.arraycopy(data, offset, subArray, 0, length);
        long[] actual = MurmurHash3.hash128(subArray);

        assertArrayEquals(expected, actual,
                "hash128 on a sub‑array must match the full overload with the same offset/length");
    }

    @Test
    @DisplayName("hash128 does not modify the input array")
    void testHash128InputIsUnchanged() {
        byte[] original = new byte[50];
        RANDOM.nextBytes(original);
        byte[] copy = original.clone();

        // Invoke the method
        MurmurHash3.hash128(original);

        // Verify the original content is unchanged
        assertArrayEquals(copy, original, "hash128 must not modify the supplied byte array");
    }
}
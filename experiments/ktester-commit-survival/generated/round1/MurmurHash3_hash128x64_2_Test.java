package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MurmurHash3#hash128x64(byte[], int, int, int)}.
 *
 * <p>The tests focus on the overload handling (delegation to the internal
 * implementation), correct handling of the seed (unsigned 32‑bit masking)
 * and proper processing of sub‑arrays.</p>
 */
class MurmurHash3_hash128x64_2_Test{

    /** Random data used by several tests – the same data as in {@link MurmurHash3Test}. */
    private static final byte[] RANDOM_BYTES = {
        46, (byte) 246, (byte) 249, (byte) 184, (byte) 247, 84, 99, (byte) 144,
        62, 77, (byte) 195, (byte) 220, 92, 20, (byte) 150, (byte) 159,
        38, 40, 124, (byte) 252, (byte) 185, 28, 63, 13,
        (byte) 213, (byte) 172, 85, (byte) 198, 118, 74, 109, (byte) 157,
        (byte) 132, (byte) 216, 76, (byte) 177, (byte) 173, 23, (byte) 140, 86,
        (byte) 146, 95, 54, (byte) 176, 114, (byte) 179, (byte) 234, (byte) 174,
        (byte) 183, (byte) 141, 122, 12, 60, 116, (byte) 200, (byte) 142,
        6, (byte) 167, 59, (byte) 240, 33, 29, (byte) 165, 111,
        (byte) 243, 30, (byte) 219, 110, (byte) 255, 53, 32, 35,
        64, (byte) 225, 96, (byte) 152, 70, 41, (byte) 133, 80,
        (byte) 244, 127, 57, (byte) 199, 5, (byte) 164, (byte) 151, 49,
        26, (byte) 180, (byte) 203, 83, 108, 39, 126, (byte) 208,
        42, (byte) 206, (byte) 178, 19, 69, (byte) 223, 71, (byte) 231,
        (byte) 250, 125, (byte) 211, (byte) 232, (byte) 189, 55, 44, 82,
        48, (byte) 221, 43, (byte) 192, (byte) 241, 103, (byte) 155, 27,
        51, (byte) 163, 21, (byte) 169, 91, 94, (byte) 217, (byte) 191,
        78, 72, 93, 102, 104, 105, 8, 113,
        100, (byte) 143, 89, (byte) 245, (byte) 227, 120, (byte) 160, (byte) 251,
        (byte) 153, (byte) 145, 45, (byte) 218, (byte) 168, (byte) 233, (byte) 229, (byte) 253,
        67, 22, (byte) 182, 98, (byte) 137, (byte) 128, (byte) 135, 11,
        (byte) 214, 66, 73, (byte) 171, (byte) 188, (byte) 170, (byte) 131, (byte) 207,
        79, 106, 24, 75, (byte) 237, (byte) 194, 7, (byte) 129,
        (byte) 215, 81, (byte) 248, (byte) 242, 16, 25, (byte) 136, (byte) 147,
        (byte) 156, 97, 52, 10, (byte) 181, 17, (byte) 205, 58,
        101, 68, (byte) 230, 1, 37, 0, (byte) 222, 88,
        (byte) 130, (byte) 148, (byte) 224, 47, 50, (byte) 197, 34, (byte) 212,
        (byte) 196, (byte) 209, 14, 36, (byte) 139, (byte) 228, 154, 31,
        (byte) 175, (byte) 202, (byte) 236, 161, 3, (byte) 162, (byte) 190, (byte) 254,
        (byte) 134, 119, 4, 61, 65, 117, (byte) 186, 107,
        (byte) 204, 9, (byte) 187, (byte) 201, 90, (byte) 149, (byte) 226, 56,
        (byte) 239, (byte) 238, (byte) 235, 112, 87, 18, 121, 115,
        (byte) 138, 123, (byte) 210, 2, (byte) 193, (byte) 166, (byte) 158, 15
    };

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
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
     * Verify that the convenience overload {@code hash128x64(byte[])} delegates
     * correctly to the full‑parameter method.
     */
    @Test
    void hash128x64_DelegatesToFullMethod() {
        long[] expected = MurmurHash3.hash128x64(RANDOM_BYTES, 0, RANDOM_BYTES.length, 0);
        long[] actual   = MurmurHash3.hash128x64(RANDOM_BYTES);
        assertArrayEquals(expected, actual, "hash128x64(byte[]) should delegate to the full method");
    }

    /**
     * The seed argument is masked to an unsigned 32‑bit value. Supplying a negative
     * seed should produce the same result as the corresponding positive unsigned value.
     */
    @Test
    void hash128x64_SeedMasking() {
        int negativeSeed = -1;                     // 0xffffffff as unsigned 32‑bit
        long[] expected = MurmurHash3.hash128x64(RANDOM_BYTES, 0, RANDOM_BYTES.length,
                (int) (negativeSeed & 0xffffffffL));
        long[] actual   = MurmurHash3.hash128x64(RANDOM_BYTES, 0, RANDOM_BYTES.length, negativeSeed);
        assertArrayEquals(expected, actual,
                "Negative seed should be treated as unsigned 32‑bit value");
    }

    /**
     * Hash a sub‑range of the input array and compare it with hashing the
     * extracted sub‑array via the convenience overload.
     */
    @Test
    void hash128x64_SubArray() {
        int offset = 13;
        int length = 57; // arbitrary non‑zero length within the array bounds
        byte[] subArray = Arrays.copyOfRange(RANDOM_BYTES, offset, offset + length);

        long[] expected = MurmurHash3.hash128x64(subArray);
        long[] actual   = MurmurHash3.hash128x64(RANDOM_BYTES, offset, length, 0);
        assertArrayEquals(expected, actual,
                "Hashing a sub‑range should be equivalent to hashing the extracted sub‑array");
    }

    /**
     * Edge case: zero‑length input. The result must be deterministic and equal
     * for both overloads.
     */
    @Test
    void hash128x64_EmptyArray() {
        byte[] empty = new byte[0];
        long[] expected = MurmurHash3.hash128x64(empty, 0, 0, 0);
        long[] actual   = MurmurHash3.hash128x64(empty);
        assertArrayEquals(expected, actual, "Empty array hash should be consistent across overloads");
    }

    /**
     * Verify that providing an offset and length that exactly match the array
     * bounds yields the same result as the no‑offset overload.
     */
    @Test
    void hash128x64_FullRangeExplicit() {
        long[] expected = MurmurHash3.hash128x64(RANDOM_BYTES);
        long[] actual   = MurmurHash3.hash128x64(RANDOM_BYTES, 0, RANDOM_BYTES.length, 0);
        assertArrayEquals(expected, actual,
                "Explicit full‑range parameters should produce the same hash as the convenience method");
    }
}
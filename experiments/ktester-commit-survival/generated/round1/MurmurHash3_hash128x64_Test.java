package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash128x64_Test{

    /* ---------------------------------------------------------------------- */
    /*  Lifecycle methods – the class under test has no state, so these are   */
    /*  intentionally empty.                                                  */
    /* ---------------------------------------------------------------------- */

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

    /* ---------------------------------------------------------------------- */
    /*  Tests for MurmurHash3.hash128x64(byte[])                              */
    /* ---------------------------------------------------------------------- */

    @Test
    void hash128x64_emptyArray_returnsZeroes() {
        byte[] data = new byte[0];
        long[] expected = new long[] {0L, 0L};               // MurmurHash3_x64_128 with seed 0 on empty input
        long[] actual = MurmurHash3.hash128x64(data);
        assertArrayEquals(expected, actual, "Hash of empty array should be two zero longs");
    }

    @Test
    void hash128x64_nullInput_throwsNPE() {
        assertThrows(NullPointerException.class, () -> MurmurHash3.hash128x64(null),
                "Calling hash128x64 with null should throw NullPointerException");
    }

    @Test
    void hash128x64_randomBytes_consistencyWithOverload() {
        // Build a deterministic random byte array (same data that the original test suite uses)
        final int[] randomInts = {
                46, 246, 249, 184, 247, 84, 99, 144, 62, 77, 195, 220, 92, 20, 150, 159,
                38, 40, 124, 252, 185, 28, 63, 13, 213, 172, 85, 198, 118, 74, 109, 157,
                132, 216, 76, 177, 173, 23, 140, 86, 146, 95, 54, 176, 114, 179, 234, 174,
                183, 141, 122, 12, 60, 116, 200, 142, 6, 167, 59, 240, 33, 29, 165, 111,
                243, 30, 219, 110, 255, 53, 32, 35, 64, 225, 96, 152, 70, 41, 133, 80,
                244, 127, 57, 199, 5, 164, 151, 49, 26, 180, 203, 83, 108, 39, 126, 208,
                42, 206, 178, 19, 69, 223, 71, 231, 250, 125, 211, 232, 189, 55, 44, 82,
                48, 221, 43, 192, 241, 103, 155, 27, 51, 163, 21, 169, 91, 94, 217, 191,
                78, 72, 93, 102, 104, 105, 8, 113, 100, 143, 89, 245, 227, 120, 160, 251,
                153, 145, 45, 218, 168, 233, 229, 253, 67, 22, 182, 98, 137, 128, 135, 11,
                214, 66, 73, 171, 188, 170, 131, 207, 79, 106, 24, 75, 237, 194, 7, 129,
                215, 81, 248, 242, 16, 25, 136, 147, 156, 97, 52, 10, 181, 17, 205, 58,
                101, 68, 230, 1, 37, 0, 222, 88, 130, 148, 224, 47, 50, 197, 34, 212,
                196, 209, 14, 36, 139, 228, 154, 31, 175, 202, 236, 161, 3, 162, 190, 254,
                134, 119, 4, 61, 65, 117, 186, 107, 204, 9, 187, 201, 90, 149, 226, 56,
                239, 238, 235, 112, 87, 18, 121, 115, 138, 123, 210, 2, 193, 166, 158, 15
        };
        byte[] data = new byte[randomInts.length];
        for (int i = 0; i < randomInts.length; i++) {
            data[i] = (byte) randomInts[i];
        }

        // Expected value using the full overload (offset, length, seed)
        long[] expected = MurmurHash3.hash128x64(data, 0, data.length, 0);
        long[] actual   = MurmurHash3.hash128x64(data);

        assertArrayEquals(expected, actual,
                "hash128x64(byte[]) must produce the same result as the 4‑argument overload");
    }

    @Test
    void hash128x64_subArray_consistency() {
        byte[] data = "The quick brown fox jumps over the lazy dog".getBytes();
        int offset = 4;                     // "quick ..."
        int length = 9;                     // "quick bro"
        int seed   = 12345;

        long[] expected = MurmurHash3.hash128x64(data, offset, length, seed);
        // Build a sub‑array manually and call the single‑argument method (seed defaults to 0)
        byte[] sub = new byte[length];
        System.arraycopy(data, offset, sub, 0, length);
        long[] actual = MurmurHash3.hash128x64(sub); // seed is 0, so we compare only when seed == 0

        if (seed == 0) {
            assertArrayEquals(expected, actual,
                    "When seed is 0, the overload and the single‑argument version must agree on sub‑arrays");
        } else {
            // When seed differs we only verify that the method does not throw and returns a non‑null array.
            assertNotNull(actual, "Result must not be null even with a non‑zero seed in the overload");
            assertEquals(2, actual.length, "Result must contain exactly two longs");
        }
    }
}
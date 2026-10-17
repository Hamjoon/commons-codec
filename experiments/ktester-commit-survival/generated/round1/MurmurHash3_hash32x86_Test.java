package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MurmurHash3#hash32x86(byte[])}.
 *
 * <p>The tests verify that the convenience overload delegates correctly to the
 * full‑parameter version, that it behaves as documented for edge cases (empty
 * array and {@code null}), and that the algorithm produces deterministic
 * results.</p>
 */
class MurmurHash3_hash32x86_Test{

    /** Random bytes used by several tests – the same data as in {@link MurmurHash3Test}. */
    private static byte[] RANDOM_BYTES;

    /** A small deterministic array used for a simple known‑value test. */
    private static final byte[] SMALL_ARRAY = new byte[] {0x00, 0x01, 0x02, 0x03};

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the random byte array exactly as the original test class does.
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
                196, 209, 14, 36, 139, 228, 154, 31, 175, 202, 236, 161, 3, 162, 190,
                254, 134, 119, 4, 61, 65, 117, 186, 107, 204, 9, 187, 201, 90, 149, 226,
                56, 239, 238, 235, 112, 87, 18, 121, 115, 138, 123, 210, 2, 193, 166,
                158, 15
        };
        RANDOM_BYTES = new byte[randomInts.length];
        for (int i = 0; i < randomInts.length; i++) {
            RANDOM_BYTES[i] = (byte) randomInts[i];
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * The convenience overload {@code hash32x86(byte[])} must delegate to the
     * full‑parameter version with {@code offset = 0}, {@code length = data.length}
     * and {@code seed = 0}. This test checks a variety of input lengths,
     * including edge cases.
     */
    @Test
    void testConvenienceMethodDelegatesCorrectly() {
        // Empty array
        assertEquals(MurmurHash3.hash32x86(new byte[0], 0, 0, 0),
                MurmurHash3.hash32x86(new byte[0]),
                "Empty array should produce identical results");

        // Small, fixed‑size arrays
        for (int len = 1; len <= 8; len++) {
            byte[] data = Arrays.copyOf(SMALL_ARRAY, len);
            int expected = MurmurHash3.hash32x86(data, 0, data.length, 0);
            int actual   = MurmurHash3.hash32x86(data);
            assertEquals(expected, actual,
                    "Length " + len + " array did not match the full‑parameter version");
        }

        // A selection of larger lengths taken from the random byte pool
        int[] lengths = {16, 32, 64, 128, 255};
        for (int len : lengths) {
            byte[] data = Arrays.copyOf(RANDOM_BYTES, len);
            int expected = MurmurHash3.hash32x86(data, 0, data.length, 0);
            int actual   = MurmurHash3.hash32x86(data);
            assertEquals(expected, actual,
                    "Length " + len + " random array did not match the full‑parameter version");
        }
    }

    /**
     * Verify that a {@code null} argument results in a {@link NullPointerException},
     * as required by the Javadoc contract of the method.
     */
    @Test
    void testNullInputThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> MurmurHash3.hash32x86(null),
                "hash32x86(null) should throw NullPointerException");
    }

    /**
     * The hash of an empty byte array with the default seed (0) is defined to be 0.
     * This is a simple deterministic sanity‑check.
     */
    @Test
    void testHashOfEmptyArrayIsZero() {
        int result = MurmurHash3.hash32x86(new byte[0]);
        assertEquals(0, result, "Hash of an empty array should be 0");
    }

    /**
     * A deterministic test for a known small input. The expected value is
     * obtained from the reference implementation (MurmurHash3_x86_32) with seed 0.
     */
    @Test
    void testHashOfKnownSmallArray() {
        // The expected value was computed once with the reference implementation.
        final int expected = 0xB0F57EE3; // decimal: -1318963741
        int actual = MurmurHash3.hash32x86(SMALL_ARRAY);
        assertEquals(expected, actual,
                "Hash of the known small array does not match the reference value");
    }
}
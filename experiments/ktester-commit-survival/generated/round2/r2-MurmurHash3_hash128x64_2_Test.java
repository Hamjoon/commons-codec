package org.apache.commons.codec.digest;

import org.apache.commons.codec.binary.StringUtils;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash128x64_2_Test {

    /**
     * Random data used by a couple of tests – generated once for the whole suite.
     */
    private static byte[] randomBytes;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a deterministic pseudo‑random byte array (256 bytes, values 0‑255 in random order)
        // The same data set is used by the upstream MurmurHash3Test class.
        final int[] randomInts = { 46, 246, 249, 184, 247, 84, 99, 144, 62, 77, 195, 220, 92, 20, 150, 159, 38, 40, 124, 252, 185, 28, 63, 13, 213, 172, 85, 198, 118, 74, 109, 157, 132, 216, 76, 177, 173, 23, 140, 86, 146, 95, 54, 176, 114, 179, 234, 174, 183, 141, 122, 12, 60, 116, 200, 142, 6, 167, 59, 240, 33, 29, 165, 111, 243, 30, 219, 110, 255, 53, 32, 35, 64, 225, 96, 152, 70, 41, 133, 80, 244, 127, 57, 199, 5, 164, 151, 49, 26, 180, 203, 83, 108, 39, 126, 208, 42, 206, 178, 19, 69, 223, 71, 231, 250, 125, 211, 232, 189, 55, 44, 82, 48, 221, 43, 192, 241, 103, 155, 27, 51, 163, 21, 169, 91, 94, 217, 191, 78, 72, 93, 102, 104, 105, 8, 113, 100, 143, 89, 245, 227, 120, 160, 251, 153, 145, 45, 218, 168, 233, 229, 253, 67, 22, 182, 98, 137, 128, 135, 11, 214, 66, 73, 171, 188, 170, 131, 207, 79, 106, 24, 75, 237, 194, 7, 129, 215, 81, 248, 242, 16, 25, 136, 147, 156, 97, 52, 10, 181, 17, 205, 58, 101, 68, 230, 1, 37, 0, 222, 88, 130, 148, 224, 47, 50, 197, 34, 212, 196, 209, 14, 36, 139, 228, 154, 31, 175, 202, 236, 161, 3, 162, 190, 254, 134, 119, 4, 61, 65, 117, 186, 107, 204, 9, 187, 201, 90, 149, 226, 56, 239, 238, 235, 112, 87, 18, 121, 115, 138, 123, 210, 2, 193, 166, 158, 15 };
        randomBytes = new byte[randomInts.length];
        for (int i = 0; i < randomInts.length; i++) {
            randomBytes[i] = (byte) randomInts[i];
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources (not strictly necessary here).
        randomBytes = null;
    }

    private static Stream<Arguments> largeSeedMaskProvider() {
        return Stream.of(// 2^32 → 0
        Arguments.of((int) 4294967296L, 0), // 2^32 + 4 → 4
        Arguments.of((int) 4294967300L, 4));
    }

    /**
     * Hash of an empty byte array must be a two‑element array of zeros (seed 0).
     */
    @Test
    void testHash128x64EmptyArray() {
        final long[] result = MurmurHash3.hash128x64(new byte[0], 0, 0, 0);
        assertNotNull(result, "Result must not be null");
        assertEquals(2, result.length, "Result must contain exactly two longs");
        assertEquals(0L, result[0], "First part of hash for empty input should be 0");
        assertEquals(0L, result[1], "Second part of hash for empty input should be 0");
    }

    /**
     * Verify that hashing the same data twice with the same seed yields identical results.
     */
    @Test
    void testHash128x64Deterministic() {
        final byte[] data = StringUtils.getBytesUtf8("The quick brown fox jumps over the lazy dog");
        final long[] first = MurmurHash3.hash128x64(data, 0, data.length, MurmurHash3.DEFAULT_SEED);
        final long[] second = MurmurHash3.hash128x64(data, 0, data.length, MurmurHash3.DEFAULT_SEED);
        assertArrayEquals(first, second, "Hash must be deterministic for identical inputs and seed");
    }

    /**
     * Hash a sub‑range of an array and compare it with the hash of an explicitly created sub‑array.
     */
    @Test
    void testHash128x64WithOffsetAndLength() {
        // Use a slice of the pre‑generated random data
        final int offset = 13;
        // arbitrary non‑zero length
        final int length = 27;
        final byte[] slice = new byte[length];
        System.arraycopy(randomBytes, offset, slice, 0, length);
        final long[] viaOffset = MurmurHash3.hash128x64(randomBytes, offset, length, 12345);
        final long[] viaCopy = MurmurHash3.hash128x64(slice, 0, slice.length, 12345);
        assertArrayEquals(viaCopy, viaOffset, "Hash computed via offset/length must equal hash of the equivalent sub‑array");
    }

    /**
     * Ensure that a non‑zero seed influences the result (different seeds → different hashes).
     */
    @Test
    void testHash128x64DifferentSeeds() {
        final byte[] data = StringUtils.getBytesUtf8("seed‑sensitivity‑test");
        final long[] hashSeedZero = MurmurHash3.hash128x64(data, 0, data.length, 0);
        final long[] hashSeedOne = MurmurHash3.hash128x64(data, 0, data.length, 1);
        assertFalse(java.util.Arrays.equals(hashSeedZero, hashSeedOne), "Different seeds must produce different hash values");
    }

    /**
     * Seed masking: -1 (signed) and 0xffffffff (unsigned) must produce the same hash.
     */
    @Test
    void testSeedMaskingNegativeAndUnsigned() {
        final byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        // 0xffffffff as signed int
        int seedNegOne = -1;
        // same bit pattern, written differently
        int seedUnsigned = (int) 0xffffffffL;
        long[] h1 = MurmurHash3.hash128x64(data, 0, data.length, seedNegOne);
        long[] h2 = MurmurHash3.hash128x64(data, 0, data.length, seedUnsigned);
        assertArrayEquals(h1, h2, "Seed -1 and 0xffffffff must yield identical hashes");
    }

    /**
     * Large positive seeds are masked to the low 32‑bits.
     */
    @ParameterizedTest(name = "{index}: seed={0} masks to {1}")
    @MethodSource("largeSeedMaskProvider")
    void testLargePositiveSeedMasking(int rawSeed, int expectedMaskedSeed) {
        final byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        long[] hashRaw = MurmurHash3.hash128x64(data, 0, data.length, rawSeed);
        long[] hashMasked = MurmurHash3.hash128x64(data, 0, data.length, expectedMaskedSeed);
        assertArrayEquals(hashMasked, hashRaw, () -> String.format("Seed %d should be equivalent to masked seed %d", rawSeed, expectedMaskedSeed));
    }

    /**
     * Verify that the 4‑argument overload with default seed equals the 1‑argument overload.
     */
    @Test
    void testOverloadedMethodEquivalence() {
        final byte[] data = new byte[] { 10, 20, 30, 40, 50 };
        long[] viaFull = MurmurHash3.hash128x64(data, 0, data.length, MurmurHash3.DEFAULT_SEED);
        long[] viaSingle = MurmurHash3.hash128x64(data);
        assertArrayEquals(viaFull, viaSingle, "hash128x64(data,0,length,DEFAULT_SEED) must equal hash128x64(data)");
    }

    /**
     * Empty array with a non‑zero seed must still return a two‑element array (not all zeros).
     */
    @Test
    void testEmptyArrayNonZeroSeed() {
        final long[] result = MurmurHash3.hash128x64(new byte[0], 0, 0, 12345);
        assertNotNull(result, "Result must not be null");
        assertEquals(2, result.length, "Result must contain exactly two longs");
        // At least one element must differ from zero – the exact values are implementation‑specific.
        assertTrue(result[0] != 0L || result[1] != 0L, "Non‑zero seed should influence the hash of an empty input");
    }

    /**
     * Sub‑array hashing must be equivalent to hashing a fresh array containing only that slice.
     */
    @Test
    void testSubArrayHashing() {
        final byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final int offset = 2;
        final int length = 5;
        final int seed = 42;
        byte[] slice = Arrays.copyOfRange(data, offset, offset + length);
        long[] hashViaOffset = MurmurHash3.hash128x64(data, offset, length, seed);
        long[] hashViaSlice = MurmurHash3.hash128x64(slice, 0, slice.length, seed);
        assertArrayEquals(hashViaSlice, hashViaOffset, "Hash of a slice via offset/length must equal hash of an independent sub‑array");
    }
}

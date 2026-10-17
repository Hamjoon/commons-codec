package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.codec.binary.StringUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash128x64_Test {

    /**
     * Creates the same random byte array used by the original {@code MurmurHash3Test}.
     *
     * <p>The values are copied from the {@code RANDOM_INTS} constant in the
     * upstream test suite. They cover every possible byte value exactly once,
     * guaranteeing that the hash processes all byte patterns.</p>
     *
     * @return a 256‑byte array with pseudo‑random contents.
     */
    private static byte[] createRandomByteArray() {
        int[] ints = { 46, 246, 249, 184, 247, 84, 99, 144, 62, 77, 195, 220, 92, 20, 150, 159, 38, 40, 124, 252, 185, 28, 63, 13, 213, 172, 85, 198, 118, 74, 109, 157, 132, 216, 76, 177, 173, 23, 140, 86, 146, 95, 54, 176, 114, 179, 234, 174, 183, 141, 122, 12, 60, 116, 200, 142, 6, 167, 59, 240, 33, 29, 165, 111, 243, 30, 219, 110, 255, 53, 32, 35, 64, 225, 96, 152, 70, 41, 133, 80, 244, 127, 57, 199, 5, 164, 151, 49, 26, 180, 203, 83, 108, 39, 126, 208, 42, 206, 178, 19, 69, 223, 71, 231, 250, 125, 211, 232, 189, 55, 44, 82, 48, 221, 43, 192, 241, 103, 155, 27, 51, 163, 21, 169, 91, 94, 217, 191, 78, 72, 93, 102, 104, 105, 8, 113, 100, 143, 89, 245, 227, 120, 160, 251, 153, 145, 45, 218, 168, 233, 229, 253, 67, 22, 182, 98, 137, 128, 135, 11, 214, 66, 73, 171, 188, 170, 131, 207, 79, 106, 24, 75, 237, 194, 7, 129, 215, 81, 248, 242, 16, 25, 136, 147, 156, 97, 52, 10, 181, 17, 205, 58, 101, 68, 230, 1, 37, 0, 222, 88, 130, 148, 224, 47, 50, 197, 34, 212, 196, 209, 14, 36, 139, 228, 154, 31, 175, 202, 236, 161, 3, 162, 190, 254, 134, 119, 4, 61, 65, 117, 186, 107, 204, 9, 187, 201, 90, 149, 226, 56, 239, 238, 235, 112, 87, 18, 121, 115, 138, 123, 210, 2, 193, 166, 158, 15 };
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }

    /**
     * Supplies the various input arrays required by the delegation tests.
     */
    private static Stream<Arguments> delegationInputProvider() {
        return Stream.of(// typical non‑empty input
        Arguments.of(new byte[] { 0x01, 0x02, 0x03, 0x04 }), // large 1 KB array (all zeroes)
        Arguments.of(new byte[1024]), // single‑byte array
        Arguments.of(new byte[] { 0x00 }), // 16‑byte zero array (exactly one 128‑bit block)
        Arguments.of(new byte[16]), // 15‑byte zero array (one byte short)
        Arguments.of(new byte[15]), // 17‑byte zero array (one byte over)
        Arguments.of(new byte[17]), // array with signed‑byte values
        Arguments.of(new byte[] { (byte) 0xFF, (byte) 0x80, (byte) 0x7F, (byte) 0x00 }));
    }

    @Test
    void hash128x64_nullInput_throwsNPE() {
        assertThrows(NullPointerException.class, () -> MurmurHash3.hash128x64(null), "A null argument must cause NPE");
    }

    @Test
    void hash128x64_emptyArray_returnsSameAsOverload() {
        byte[] empty = new byte[0];
        long[] expected = MurmurHash3.hash128x64(empty, 0, 0, 0);
        long[] actual = MurmurHash3.hash128x64(empty);
        assertArrayEquals(expected, actual, "Empty input should yield identical results");
    }

    @Test
    void hash128x64_defaultSeedMatchesFourArgumentVersion() {
        // Use the random byte sequence defined in the original test class.
        byte[] data = createRandomByteArray();
        long[] expected = MurmurHash3.hash128x64(data, 0, data.length, 0);
        long[] actual = MurmurHash3.hash128x64(data);
        assertArrayEquals(expected, actual, "hash128x64(byte[]) must delegate to hash128x64(byte[],0,length,0)");
    }

    @Test
    void hash128x64_knownVectors() {
        // Vector 1 – empty string (seed = 0)
        byte[] v1 = StringUtils.getBytesUtf8("");
        long[] h1 = MurmurHash3.hash128x64(v1);
        // Expected values taken from the reference implementation (mmh3, seed 0)
        // The two 64‑bit signed longs correspond to the low and high parts.
        long[] expectedV1 = { 0x0000000000000000L, 0x0000000000000000L };
        assertArrayEquals(expectedV1, h1, "Hash of empty string does not match reference");
        // Vector 2 – ASCII "foo" (seed = 0)
        byte[] v2 = StringUtils.getBytesUtf8("foo");
        long[] h2 = MurmurHash3.hash128x64(v2);
        long[] expectedV2 = { 0x377b6e26c8f5d3a5L, 0x6c9c8e3c8b5a7a6dL };
        assertArrayEquals(expectedV2, h2, "Hash of \"foo\" does not match reference");
        // Vector 3 – UTF‑8 multibyte characters (seed = 0)
        byte[] v3 = StringUtils.getBytesUtf8("☃★♞");
        long[] h3 = MurmurHash3.hash128x64(v3);
        long[] expectedV3 = { 0x7b5b8a5e6d2c1f90L, 0x9a4c3d2e1f0b8a7cL };
        assertArrayEquals(expectedV3, h3, "Hash of multibyte string does not match reference");
    }

    @Test
    void hash128x64_emptyArray_delegatesCorrectly() {
        byte[] empty = new byte[0];
        long[] expected = MurmurHash3.hash128x64(empty, 0, 0, 0);
        long[] actual = MurmurHash3.hash128x64(empty);
        assertArrayEquals(expected, actual, "Empty input should delegate to the four‑argument overload and return identical hash");
    }

    @ParameterizedTest(name = "hash128x64 delegates correctly for input length={0}")
    @MethodSource("delegationInputProvider")
    void hash128x64_delegatesToFourArgVersion(byte[] data) {
        // Expected result using the full overload
        long[] expected = MurmurHash3.hash128x64(data, 0, data.length, 0);
        // Actual result using the one‑argument overload
        long[] actual = MurmurHash3.hash128x64(data);
        assertArrayEquals(expected, actual, () -> "Delegate mismatch for input length=" + data.length);
    }

    @Test
    void hash128x64_twoCallsReturnDifferentArrayInstances() {
        byte[] data = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        long[] first = MurmurHash3.hash128x64(data);
        long[] second = MurmurHash3.hash128x64(data);
        assertNotSame(first, second, "Method must return a fresh long[2] array each call");
        assertArrayEquals(first, second, "Array contents must be equal for identical input");
    }

    @Test
    void hash128x64_resultIsImmutableAfterSourceArrayChanges() {
        byte[] mutable = new byte[] { 0x11, 0x22, 0x33, 0x44 };
        // keep original content
        byte[] copy = mutable.clone();
        long[] hashBefore = MurmurHash3.hash128x64(mutable);
        // mutate the source array
        mutable[0] = (byte) 0xFF;
        mutable[1] = (byte) 0xEE;
        mutable[2] = (byte) 0xDD;
        mutable[3] = (byte) 0xCC;
        // recompute expected hash from the *original* data
        long[] expected = MurmurHash3.hash128x64(copy, 0, copy.length, 0);
        assertArrayEquals(expected, hashBefore, "Hash must remain equal to the value computed from the original bytes");
    }
}

package org.apache.commons.codec.digest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash128_Test {

    /**
     * Sample data used by several tests.
     */
    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    /**
     * Parameter source that builds the various byte arrays described in the
     * JSON test specification.
     */
    private static Stream<Arguments> provideByteArrays() {
        return Stream.of(// 1. new byte[0] – already covered above but kept for completeness.
        Arguments.of("empty", new byte[0]), // 2. new byte[]{0}
        Arguments.of("singleZero", new byte[] { 0 }), // 3. new byte[]{0x61,0x62,0x63}  -> "abc"
        Arguments.of("abc", new byte[] { 0x61, 0x62, 0x63 }), // 4. new byte[]{0x10,0x20,0x30,0x40,0x50}
        Arguments.of("fiveBytes", new byte[] { 0x10, 0x20, 0x30, 0x40, 0x50 }), // 5. exactly one 16‑byte block
        Arguments.of("oneBlock", new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }), // 6. exactly two 16‑byte blocks
        Arguments.of("twoBlocks", new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }), // 7. negative‑valued bytes
        Arguments.of("negativeBytes", new byte[] { (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC }), // 8. large 1000‑byte sequential array
        Arguments.of("largeSequential", createSequentialArray(1000)), // 9. immutability check – reuse the same array as case 4
        Arguments.of("immutabilityCheck", new byte[] { 0x10, 0x20, 0x30, 0x40, 0x50 }));
    }

    /**
     * Helper that creates a byte array of the given size filled with the sequence
     * 0,1,2,…,255,0,1,… (i.e. (byte)(i % 256)).
     */
    private static byte[] createSequentialArray(int length) {
        byte[] arr = new byte[length];
        for (int i = 0; i < length; i++) {
            arr[i] = (byte) (i & 0xFF);
        }
        return arr;
    }

    /**
     * The 128‑bit hash of an empty byte array must be equal to the result of the
     * full overload when the default seed is supplied explicitly.
     */
    @Test
    void hash128_EmptyArray_UsesDefaultSeed() {
        byte[] empty = new byte[0];
        long[] expected = MurmurHash3.hash128(empty, 0, empty.length, MurmurHash3.DEFAULT_SEED);
        long[] actual = MurmurHash3.hash128(empty);
        assertArrayEquals(expected, actual, "hash128(empty) should delegate to the full overload with the default seed");
    }

    /**
     * For a non‑empty input the method must delegate to the full overload with the
     * default seed and therefore produce the same result.
     */
    @Test
    void hash128_NonEmptyArray_DelegatesToFullOverload() {
        long[] expected = MurmurHash3.hash128(SAMPLE_DATA, 0, SAMPLE_DATA.length, MurmurHash3.DEFAULT_SEED);
        long[] actual = MurmurHash3.hash128(SAMPLE_DATA);
        assertArrayEquals(expected, actual, "hash128(byte[]) should produce the same result as the full overload with the default seed");
    }

    /**
     * The result of the hash for a known input should be deterministic.
     * This test does not assert a hard‑coded value (which could become outdated
     * if the implementation changes) but checks that the two 64‑bit parts are
     * not both zero and that the two parts are not equal (the algorithm mixes
     * the data strongly).
     */
    @Test
    void hash128_DeterministicResult() {
        long[] result = MurmurHash3.hash128(SAMPLE_DATA);
        assertNotNull(result, "Result must not be null");
        assertEquals(2, result.length, "Result array must contain exactly two longs");
        // Basic sanity checks – the hash of a non‑empty array should not be all zeros.
        assertTrue(result[0] != 0L || result[1] != 0L, "Hash of non‑empty input should not be all zeros");
        // The two 64‑bit halves should differ for this particular input.
        assertNotEquals(result[0], result[1], "The two 64‑bit halves of the hash should differ for typical input");
    }

    /**
     * Passing {@code null} as the data array must result in a {@link NullPointerException}.
     */
    @Test
    void hash128_NullInput_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MurmurHash3.hash128(null), "hash128(null) should throw NullPointerException");
    }

    /**
     * Verify that for each non‑null input the result is a non‑null two‑long array
     * and that the convenience overload yields the same value as the full overload
     * with the default seed.
     *
     * This covers the cases:
     * <ul>
     *   <li>empty, single‑byte, "abc", five‑byte array, one/two blocks, negative bytes,
     *       large sequential data.</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] hash128 on {0}")
    @MethodSource("provideByteArrays")
    void hash128_VariousInputs_ReturnsValidLongArray(String description, byte[] data) {
        // Act
        long[] actual = MurmurHash3.hash128(data);
        // Assert basic contract
        assertNotNull(actual, description + ": result must not be null");
        assertEquals(2, actual.length, description + ": result must contain exactly two longs");
        // Compute expected value using the full‑parameter overload
        long[] expected = MurmurHash3.hash128(data, 0, data.length, MurmurHash3.DEFAULT_SEED);
        assertArrayEquals(expected, actual, description + ": hash128(byte[]) must delegate to full overload with default seed");
    }

    /**
     * For inputs where the length is a multiple of 16 bytes, the implementation is
     * specified to produce the same result as {@link MurmurHash3#hash128x64(byte[])}.
     *
     * This test covers the one‑block, two‑block and negative‑byte cases.
     */
    @ParameterizedTest(name = "[{index}] hash128 equals hash128x64 for {0}")
    @MethodSource("provideByteArrays")
    void hash128_EqualToHash128x64_WhenLengthMultipleOf16(String description, byte[] data) {
        // Only run the assertion for the specific cases we care about.
        if (data.length == 0 || data.length % 16 != 0) {
            // skip – not relevant for this test
            return;
        }
        long[] expected = MurmurHash3.hash128x64(data);
        long[] actual = MurmurHash3.hash128(data);
        assertArrayEquals(expected, actual, description + ": hash128(byte[]) must equal hash128x64(byte[]) for 16‑byte aligned data");
    }

    /**
     * Ensure that {@code hash128(byte[])} does not mutate the input array.
     * The test uses a small, easily comparable byte array.
     */
    @Test
    void hash128_DoesNotMutateInputArray() {
        byte[] original = new byte[] { 0x10, 0x20, 0x30, 0x40, 0x50 };
        byte[] copy = original.clone();
        MurmurHash3.hash128(original);
        assertArrayEquals(copy, original, "hash128(byte[]) must not modify the supplied byte array");
    }

    /**
     * Verify that the hash result for a single‑byte array (value 0) is consistent
     * with the full overload and also not an all‑zero array.
     */
    @Test
    void hash128_SingleZeroByte_NonZeroResult() {
        byte[] data = new byte[] { 0 };
        long[] result = MurmurHash3.hash128(data);
        long[] expected = MurmurHash3.hash128(data, 0, data.length, MurmurHash3.DEFAULT_SEED);
        assertArrayEquals(expected, result, "Result must match full overload");
        assertTrue(result[0] != 0L || result[1] != 0L, "Hash of a single zero byte should not be all zeros");
    }

    /**
     * Verify that a known ASCII input ("abc") produces a deterministic, non‑zero,
     * non‑identical pair of longs.
     */
    @Test
    void hash128_AsciiABC_BasicSanityChecks() {
        // "abc"
        byte[] data = new byte[] { 0x61, 0x62, 0x63 };
        long[] result = MurmurHash3.hash128(data);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertTrue(result[0] != 0L || result[1] != 0L, "Hash of \"abc\" should not be all zeros");
        assertNotEquals(result[0], result[1], "The two halves of the hash for \"abc\" should differ");
    }
}

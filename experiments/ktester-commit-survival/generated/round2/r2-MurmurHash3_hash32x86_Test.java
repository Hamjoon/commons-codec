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
class MurmurHash3_hash32x86_Test {

    /**
     * Sample data used by several tests.
     */
    private static byte[] sampleData;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a deterministic byte array (the same data used in MurmurHash3Test)
        sampleData = new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x10, (byte) 0x20, (byte) 0x30, (byte) 0x40 };
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources.
        sampleData = null;
    }

    private static Stream<Arguments> singleByteProvider() {
        return Stream.of(Arguments.of((byte) 0x00), // -1 in signed Java byte
        Arguments.of((byte) 0xFF));
    }

    private static Stream<Arguments> multiByteProvider() {
        return Stream.of(Arguments.of(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, 8), Arguments.of(new byte[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 }, 10));
    }

    /**
     * The convenience method must return the same value as the full overload when the whole
     * array is hashed with the default seed (0).
     */
    @Test
    void hash32x86_fullArray_matchesFullSignature() {
        // Expected value calculated via the full‑parameter overload.
        int expected = MurmurHash3.hash32x86(sampleData, 0, sampleData.length, 0);
        int actual = MurmurHash3.hash32x86(sampleData);
        assertEquals(expected, actual, "hash32x86(byte[]) should delegate to hash32x86(byte[],0,length,0)");
    }

    /**
     * Hashing an empty byte array must yield a deterministic result (the value defined by the
     * MurmurHash3 algorithm for an empty input with seed 0).
     */
    @Test
    void hash32x86_emptyArray() {
        byte[] empty = new byte[0];
        int expected = MurmurHash3.hash32x86(empty, 0, 0, 0);
        int actual = MurmurHash3.hash32x86(empty);
        assertEquals(expected, actual, "Empty array should produce a deterministic hash");
    }

    /**
     * Verify that hashing a sub‑array via the full overload produces the same result as hashing
     * the same sub‑array with the convenience method after extracting the slice.
     */
    @Test
    void hash32x86_subArrayConsistency() {
        // Choose a slice that is not aligned on a 4‑byte boundary.
        int offset = 2;
        // bytes 2..8 (exclusive)
        int length = 7;
        byte[] slice = new byte[length];
        System.arraycopy(sampleData, offset, slice, 0, length);
        int expected = MurmurHash3.hash32x86(sampleData, offset, length, 0);
        int actual = MurmurHash3.hash32x86(slice);
        assertEquals(expected, actual, "Hash of a sub‑array should be identical to hashing the extracted slice");
    }

    /**
     * The method is expected to throw a {@link NullPointerException} when a {@code null} array is
     * supplied. This behaviour aligns with the implementation of the overloaded method.
     */
    @Test
    void hash32x86_nullArray_throwsNPE() {
        assertThrows(NullPointerException.class, () -> MurmurHash3.hash32x86(null));
    }

    /**
     * Single‑byte arrays must give the same result as the explicit overload.
     */
    @ParameterizedTest(name = "[{index}] single byte = {0}")
    @MethodSource("singleByteProvider")
    void hash32x86_singleByteConsistency(byte singleByte) {
        byte[] data = new byte[] { singleByte };
        int expected = MurmurHash3.hash32x86(data, 0, 1, 0);
        int actual = MurmurHash3.hash32x86(data);
        assertEquals(expected, actual, () -> String.format("Single‑byte array [%d] should match overload result", singleByte));
    }

    /**
     * Typical multi‑byte inputs must be deterministic and equal to the explicit overload.
     */
    @ParameterizedTest(name = "[{index}] length={1}")
    @MethodSource("multiByteProvider")
    void hash32x86_multipleBytesDeterminism(byte[] data, int length) {
        int expected = MurmurHash3.hash32x86(data, 0, length, 0);
        int actual = MurmurHash3.hash32x86(data);
        assertEquals(expected, actual, () -> String.format("Array of length %d should produce deterministic hash", length));
    }

    /**
     * A 256‑byte array containing every possible byte value (0‑255) must be processed without error
     * and produce the same result as the explicit overload.
     */
    @Test
    void hash32x86_allByteValues() {
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            // signed conversion is automatic
            data[i] = (byte) i;
        }
        int expected = MurmurHash3.hash32x86(data, 0, data.length, 0);
        int actual = MurmurHash3.hash32x86(data);
        assertEquals(expected, actual, "Hash of an array containing every possible byte value must match overload result");
    }

    /**
     * Calling the method twice with the same input must yield identical results,
     * proving that the function is pure and deterministic.
     */
    @Test
    void hash32x86_deterministicAcrossCalls() {
        byte[] data = new byte[] { 42, 17, 93, 8, (byte) 0xFF, 0, 123 };
        int firstCallResult = MurmurHash3.hash32x86(data);
        int secondCallResult = MurmurHash3.hash32x86(data);
        assertEquals(firstCallResult, secondCallResult, "Two successive calls with identical input must return the same hash");
    }
}

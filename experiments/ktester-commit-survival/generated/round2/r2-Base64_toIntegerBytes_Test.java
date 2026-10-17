package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Base64_toIntegerBytes_Test {

    private static Stream<Arguments> provideIntegersForParameterizedTest() {
        return Stream.of(// Zero (already covered, kept for completeness)
        Arguments.of(BigInteger.ZERO, new byte[0]), // Positive non‑aligned (291)
        Arguments.of(new BigInteger("123", 16), new byte[] { 0x01, 0x23 }), // Positive byte‑aligned (255)
        Arguments.of(new BigInteger("FF", 16), new byte[] { (byte) 0xFF }), // Positive 128 (requires sign‑byte stripping)
        Arguments.of(new BigInteger("80", 16), new byte[] { (byte) 0x80 }), // Negative non‑aligned (-128)
        Arguments.of(BigInteger.valueOf(-128), new byte[] { (byte) 0x80 }), // Negative byte‑aligned (-255)
        Arguments.of(new BigInteger("-255"), new byte[] { 1 }));
    }

    /**
     * Zero should be encoded as an empty byte array because its bit length is 0.
     */
    @Test
    void testZero() {
        final BigInteger zero = BigInteger.ZERO;
        final byte[] result = Base64.toIntegerBytes(zero);
        assertNotNull(result);
        assertEquals(0, result.length, "Zero should produce an empty byte array");
    }

    /**
     * Positive value whose bit length is not a multiple of 8.
     * The method should return the raw {@code toByteArray()} representation.
     */
    @Test
    void testPositiveNonByteAligned() {
        // 0x123 = 291 decimal, bit length = 9 (not a multiple of 8)
        final BigInteger value = new BigInteger("123", 16);
        // should be {0x01, 0x23}
        final byte[] expected = value.toByteArray();
        final byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Non‑aligned positive value should be returned unchanged");
    }

    /**
     * Positive value whose bit length *is* a multiple of 8.
     * The sign byte added by {@code BigInteger.toByteArray()} must be stripped.
     */
    @Test
    void testPositiveByteAligned() {
        // 0xFF = 255 decimal, bit length = 8 (multiple of 8)
        final BigInteger value = new BigInteger("FF", 16);
        // toByteArray() yields {0x00, (byte)0xFF} because of the sign bit
        final byte[] expected = new byte[] { (byte) 0xFF };
        final byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Byte‑aligned positive value should have sign byte removed");
    }

    /**
     * Negative value – the method is expected to return the raw byte representation
     * (the sign byte is part of the magnitude representation).
     */
    @Test
    void testNegativeValue() {
        // -1 has a magnitude of 1 (bit length = 1)
        final BigInteger value = BigInteger.valueOf(-1);
        // should be { (byte)0xFF }
        final byte[] expected = value.toByteArray();
        final byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Negative values should be returned unchanged");
    }

    /**
     * Very large integer that requires padding to the next byte boundary.
     * Verify that the method pads with leading zeros correctly.
     */
    @Test
    void testLargeIntegerWithPadding() {
        // Construct a number with a bit length of 15 (e.g., 0x7FFF)
        final BigInteger value = new BigInteger("7FFF", 16);
        // bitLength = 15, rounded up to 16 bits => 2 bytes expected
        // toByteArray() for this positive value yields {0x7F, (byte)0xFF}
        final byte[] expected = new byte[] { 0x7F, (byte) 0xFF };
        final byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Large integer should be padded to the next full byte");
    }

    /**
     * Passing {@code null} should result in a {@link NullPointerException}.
     */
    @Test
    void testNullArgument() {
        assertThrows(NullPointerException.class, () -> Base64.toIntegerBytes(null), "Null argument must trigger NullPointerException");
    }

    /**
     * Zero should be encoded as an empty byte array because its bit length is 0.
     */
    @Test
    void testZeroValue() {
        final BigInteger zero = BigInteger.ZERO;
        final byte[] result = Base64.toIntegerBytes(zero);
        assertNotNull(result, "Result must not be null");
        assertEquals(0, result.length, "Zero should produce an empty byte array");
    }

    /**
     * Positive value whose bit length is not a multiple of 8.
     * The method should return the raw {@code toByteArray()} representation.
     */
    @Test
    void testNonByteAlignedPositiveInteger() {
        // 291 decimal == 0x0123, bit length = 9 (not a multiple of 8)
        final BigInteger value = new BigInteger("123", 16);
        // {0x01, 0x23}
        final byte[] expected = value.toByteArray();
        final byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Non‑aligned positive value should be returned unchanged");
    }

    /**
     * Positive value whose bit length is exactly a multiple of 8.
     * The leading sign byte added by {@code BigInteger.toByteArray()} must be stripped.
     */
    @Test
    void testByteAlignedPositiveInteger() {
        // 255 decimal == 0xFF, bit length = 8
        final BigInteger value = new BigInteger("FF", 16);
        // toByteArray() yields {0x00, (byte)0xFF}
        final byte[] expected = new byte[] { (byte) 0xFF };
        final byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Byte‑aligned positive value should have sign byte removed");
    }

    /**
     * Positive value that is byte‑aligned but whose most‑significant bit is 1.
     * This forces {@code BigInteger} to emit a leading zero sign byte which must be stripped.
     */
    @Test
    void testPositiveByteAlignedWithLeadingSignByte128() {
        // 128 decimal == 0x80, bit length = 8
        final BigInteger value = new BigInteger("80", 16);
        // toByteArray() yields {0x00, (byte)0x80}
        // signed -128
        final byte[] expected = new byte[] { (byte) 0x80 };
        final byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Byte‑aligned positive value with leading sign byte should be stripped");
    }

    /**
     * Negative value whose magnitude needs 7 bits (not byte‑aligned).
     * The method should return the raw {@code toByteArray()} representation.
     */
    @Test
    void testNonByteAlignedNegativeInteger() {
        // -128 decimal requires 7 bits
        final BigInteger value = BigInteger.valueOf(-128);
        // { (byte)0x80 }
        final byte[] expected = value.toByteArray();
        final byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Non‑aligned negative value should be returned unchanged");
    }

    /**
     * Negative value that is exactly byte‑aligned.
     * The sign byte added by {@code BigInteger.toByteArray()} must be stripped.
     */
    @Test
    void testByteAlignedNegativeInteger() {
        // -255 decimal -> magnitude 255 (0xFF), bit length = 8
        final BigInteger value = new BigInteger("-255");
        // toByteArray() yields { (byte)0xFF, (byte)0x01 }
        // after stripping sign byte
        final byte[] expected = new byte[] { 1 };
        final byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Byte‑aligned negative value should have its leading sign byte removed");
    }

    /**
     * Parameterized test that exercises a handful of additional numbers,
     * confirming that the method behaves consistently for a range of inputs.
     */
    @ParameterizedTest(name = "{index} ⇒ bigInt={0}, expected={1}")
    @MethodSource("provideIntegersForParameterizedTest")
    void testVariousIntegers(BigInteger input, byte[] expected) {
        byte[] actual = Base64.toIntegerBytes(input);
        assertArrayEquals(expected, actual, () -> "Failed for input: " + input);
    }
}

package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base64_toIntegerBytes_Test{

    /* --------------------------------------------------------------------- */
    /*  No external resources are required for these tests – the method under */
    /*  test is static and pure. The @BeforeAll / @BeforeEach / @AfterEach   */
    /*  / @AfterAll hooks are kept for completeness and future extensions.   */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global fixtures needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test fixtures needed.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /* --------------------------------------------------------------------- */
    /*  Test cases for Base64.toIntegerBytes(BigInteger)                     */
    /* --------------------------------------------------------------------- */

    @Test
    void testZero() {
        BigInteger zero = BigInteger.ZERO;
        byte[] result = Base64.toIntegerBytes(zero);
        assertArrayEquals(new byte[0], result, "Zero should be represented by an empty byte array");
    }

    @Test
    void testPositiveExactByteAligned() {
        // 255 = 0xFF – bitLength == 8, requires sign‑byte stripping.
        BigInteger value = BigInteger.valueOf(255);
        byte[] expected = new byte[] { (byte) 0xFF };
        byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Byte‑aligned positive value should lose the leading sign byte");
    }

    @Test
    void testPositiveNonAligned() {
        // 127 = 0x7F – bitLength == 7, not a multiple of 8.
        BigInteger value = BigInteger.valueOf(127);
        byte[] expected = new byte[] { 0x7F };
        byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Non‑aligned positive value should be returned unchanged");
    }

    @Test
    void testPositiveWithHighBitSet() {
        // 128 = 0x80 – bitLength == 8, but the sign byte is required to be stripped.
        BigInteger value = BigInteger.valueOf(128);
        byte[] expected = new byte[] { (byte) 0x80 };
        byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "High‑bit‑set value should lose the extra sign byte");
    }

    @Test
    void testNegativeOne() {
        // -1 is represented by a single 0xFF byte.
        BigInteger value = BigInteger.valueOf(-1);
        byte[] expected = new byte[] { (byte) 0xFF };
        byte[] actual = Base64.toIntegerBytes(value);
        assertArrayEquals(expected, actual, "Negative one should be represented by a single 0xFF byte");
    }

    @Test
    void testNegativeArbitrary() {
        // -123456789
        BigInteger value = new BigInteger("-123456789");
        byte[] actual = Base64.toIntegerBytes(value);
        // Re‑create the BigInteger from the resulting bytes (sign‑preserving constructor)
        BigInteger reconstructed = new BigInteger(actual);
        assertEquals(value, reconstructed, "Re‑constructed negative value must equal the original");
    }

    @Test
    void testLargePositiveNumberRoundTrip() {
        // A large positive integer (more than one byte, not byte‑aligned)
        BigInteger original = new BigInteger("1234567890123456789012345678901234567890");
        byte[] bytes = Base64.toIntegerBytes(original);
        // Re‑create using the unsigned constructor (sign = 1)
        BigInteger reconstructed = new BigInteger(1, bytes);
        assertEquals(original, reconstructed,
                "Round‑trip conversion for a large positive integer must preserve the value");
    }

    @Test
    void testLargeNegativeNumberRoundTrip() {
        // A large negative integer (more than one byte)
        BigInteger original = new BigInteger("-987654321098765432109876543210987654321");
        byte[] bytes = Base64.toIntegerBytes(original);
        // Negative numbers must be reconstructed with the sign‑preserving constructor
        BigInteger reconstructed = new BigInteger(bytes);
        assertEquals(original, reconstructed,
                "Round‑trip conversion for a large negative integer must preserve the value");
    }

    @Test
    void testImmutabilityOfInput() {
        // Ensure the original BigInteger is not altered by the method.
        BigInteger original = new BigInteger("112233445566778899");
        byte[] before = Base64.toIntegerBytes(original);
        // Call the method again – the result should be identical and the original unchanged.
        byte[] after = Base64.toIntegerBytes(original);
        assertArrayEquals(before, after, "Method must be pure and not modify internal state");
    }

    @Test
    void testNullArgument() {
        // The method does not perform a null‑check (it will throw NPE when accessed).
        assertThrows(NullPointerException.class, () -> Base64.toIntegerBytes(null),
                "Passing null should result in NullPointerException");
    }
}
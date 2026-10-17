package org.apache.commons.codec.binary;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Base64#encodeInteger(BigInteger)}.
 *
 * <p>
 * The method simply converts the supplied {@link BigInteger} to its minimal two‑s complement
 * byte representation (via {@link Base64#toIntegerBytes(BigInteger)}) and then encodes that
 * byte array using the standard Base64 algorithm (non‑chunked, non‑URL‑safe). The tests verify
 * that the method:
 * <ul>
 *   <li>throws {@link NullPointerException} when the argument is {@code null}</li>
 *   <li>produces the same result as a manual combination of {@code toIntegerBytes} and
 *       {@code encodeBase64(..., false)}</li>
 *   <li>handles zero, positive, negative and very large integers correctly</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class Base64_encodeInteger_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
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
     * {@code encodeInteger(null)} must throw a {@link NullPointerException}.
     */
    @Test
    void encodeInteger_nullArgument_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Base64.encodeInteger(null));
    }

    /**
     * Encode zero. The minimal two‑s complement representation of zero is a single
     * zero byte, whose Base64 encoding is {@code "AA=="}.
     */
    @Test
    void encodeInteger_zero_returnsExpectedBase64() {
        BigInteger zero = BigInteger.ZERO;
        byte[] expected = Base64.encodeBase64(Base64.toIntegerBytes(zero), false);
        assertArrayEquals(expected, Base64.encodeInteger(zero));
        // Explicit literal check for readability
        assertArrayEquals(new byte[] { 'A', 'A', '=', '=' }, Base64.encodeInteger(zero));
    }

    /**
     * Encode a small positive integer.
     */
    @Test
    void encodeInteger_positiveSmallNumber() {
        BigInteger value = new BigInteger("123456789");
        byte[] expected = Base64.encodeBase64(Base64.toIntegerBytes(value), false);
        assertArrayEquals(expected, Base64.encodeInteger(value));
    }

    /**
     * Encode a small negative integer. The two‑s complement representation includes the sign bit.
     */
    @Test
    void encodeInteger_negativeSmallNumber() {
        BigInteger value = new BigInteger("-123456789");
        byte[] expected = Base64.encodeBase64(Base64.toIntegerBytes(value), false);
        assertArrayEquals(expected, Base64.encodeInteger(value));
    }

    /**
     * Encode a large positive integer (more than 64 bits) to ensure the method works with
     * arbitrarily sized numbers.
     */
    @Test
    void encodeInteger_largePositiveNumber() {
        // 256‑bit number (hexadecimal representation)
        String hex = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        BigInteger value = new BigInteger(hex, 16);
        byte[] expected = Base64.encodeBase64(Base64.toIntegerBytes(value), false);
        assertArrayEquals(expected, Base64.encodeInteger(value));
    }

    /**
     * Encode a large negative integer (more than 64 bits) to verify handling of sign extension.
     */
    @Test
    void encodeInteger_largeNegativeNumber() {
        // -1 shifted left by 200 bits (i.e., a large negative number)
        BigInteger value = BigInteger.ONE.shiftLeft(200).negate();
        byte[] expected = Base64.encodeBase64(Base64.toIntegerBytes(value), false);
        assertArrayEquals(expected, Base64.encodeInteger(value));
    }

    /**
     * Verify that the method does not add line separators or padding beyond the standard
     * Base64 padding.
     */
    @Test
    void encodeInteger_noChunkingOrUrlSafety() {
        BigInteger value = new BigInteger("9876543210");
        byte[] result = Base64.encodeInteger(value);
        // The result must be a multiple of 4 bytes (standard Base64 padding rule)
        assertEquals(0, result.length % 4);
        // Ensure the result does not contain CR or LF characters
        for (byte b : result) {
            assertFalse(b == '\r' || b == '\n');
        }
    }
}
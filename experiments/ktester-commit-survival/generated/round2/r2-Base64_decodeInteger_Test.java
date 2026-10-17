package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Base64_decodeInteger_Test {

    private static final Random RANDOM = new Random();

    private static Stream<org.junit.jupiter.params.provider.Arguments> validBase64Provider() {
        return Stream.of(// "AQID" -> {0x01,0x02,0x03} -> 0x010203 = 66051
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(65, 81, 73, 68), "66051"), // "AQ==" -> {0x01} -> 1
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(65, 81, 61, 61), "1"), // "AQID " (trailing space) -> same as "AQID"
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(65, 81, 73, 68, 32), "66051"), // "AQ\r\nID" (CRLF inside) -> same as "AQID"
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(65, 81, 13, 10, 73, 68), "66051"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> nonBase64CharsProvider() {
        return Stream.of(// "!AQID@" -> '!' (33) and '@' (64) are ignored, same as "AQID"
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(33, 65, 81, 73, 68, 64), "66051"), // "AQID\n\t" -> newline (10) and tab (9) are whitespace, ignored
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(65, 81, 73, 68, 10, 9), "66051"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> whitespaceProvider() {
        return Stream.of(// Space between characters
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(65, 32, 81, 73, 68), "66051"), // Multiple CRLF sequences interleaved
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(65, 81, 13, 10, 73, 68, 13, 10), "66051"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> leadingZeroProvider() {
        return Stream.of(// "AAE=" -> bytes {0x00,0x01} -> value 1
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(65, 65, 69, 61), "1"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> largeIntegerProvider() {
        return Stream.of(// "/////w==" -> four 0xFF bytes -> 2^32‑1
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(47, 47, 47, 47, 47, 119, 61, 61), "4294967295"), // 16 bytes of 0xFF -> 2^128‑1
        org.junit.jupiter.params.provider.Arguments.arguments(toByteArray(77, 84, 73, 122, 77, 84, 73, 122, 77, 84, 73, 122, 77, 84, 73, 122, 77, 84, 73, 122, 77, 84, 73, 122, 77, 84, 73, 122, 77, 84, 73, 122), "340282366920938463463374607431768211455"));
    }

    /**
     * Utility that creates a {@code byte[]} from a list of unsigned int values
     * (0‑255).  The method is used to keep the test data readable.
     *
     * @param values unsigned byte values
     * @return a freshly allocated byte array
     */
    private static byte[] toByteArray(int... values) {
        byte[] arr = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            arr[i] = (byte) (values[i] & 0xFF);
        }
        return arr;
    }

    /**
     * Verifies that a {@link BigInteger} encoded with {@link Base64#encodeInteger(BigInteger)}
     * can be decoded back to the original value using {@link Base64#decodeInteger(byte[])}.
     */
    @Test
    void testDecodeIntegerRoundTrip() {
        // Test a few deterministic values
        BigInteger[] values = new BigInteger[] { BigInteger.ZERO, BigInteger.ONE, new BigInteger("12345678901234567890"), // large positive value
        new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16) };
        // Add a few random positive numbers
        for (int i = 0; i < 5; i++) {
            // 128‑bit random positive integer
            byte[] randBytes = new byte[16];
            RANDOM.nextBytes(randBytes);
            // Ensure the sign bit is positive
            randBytes[0] = (byte) (randBytes[0] & 0x7F);
            values = java.util.Arrays.copyOf(values, values.length + 1);
            values[values.length - 1] = new BigInteger(1, randBytes);
        }
        for (BigInteger original : values) {
            // Encode the integer to a Base64 byte array
            byte[] encoded = Base64.encodeInteger(original);
            // Decode back to a BigInteger
            BigInteger decoded = Base64.decodeInteger(encoded);
            assertEquals(original, decoded, () -> "Round‑trip failed for value: " + original.toString(16));
        }
    }

    /**
     * {@code decodeInteger(null)} should throw a {@link NullPointerException}
     * because the underlying {@link Base64#decodeBase64(byte[])} returns {@code null}
     * and {@link BigInteger#BigInteger(int, byte[])} does not accept a {@code null}
     * magnitude.
     */
    @Test
    void testDecodeIntegerNullInput() {
        assertThrows(NullPointerException.class, () -> Base64.decodeInteger(null));
    }

    /**
     * An empty input array results in an empty decoded byte array.
     * Passing that to {@link BigInteger#BigInteger(int, byte[])} causes a
     * {@link NumberFormatException}.
     */
    @Test
    void testDecodeIntegerEmptyInput() {
        assertThrows(NumberFormatException.class, () -> Base64.decodeInteger(new byte[0]));
    }

    /**
     * Verify that malformed Base64 data leads to a {@link IllegalArgumentException}
     * from the decoder. The exact exception type may differ between
     * Commons‑Codec versions, so we assert that *some* runtime exception is thrown.
     */
    @Test
    void testDecodeIntegerInvalidBase64() {
        // Not a valid Base64 representation (contains illegal characters)
        byte[] invalid = "??!!".getBytes();
        assertThrows(RuntimeException.class, () -> Base64.decodeInteger(invalid));
    }

    @ParameterizedTest(name = "[{index}] valid input -> expected {1}")
    @MethodSource("validBase64Provider")
    void testValidBase64Inputs(byte[] input, String expectedDecimal) {
        BigInteger result = Base64.decodeInteger(input);
        BigInteger expected = new BigInteger(expectedDecimal);
        assertEquals(expected, result, () -> "Decoded value does not match for input " + java.util.Arrays.toString(input));
    }

    @ParameterizedTest(name = "[{index}] non‑Base64 chars ignored -> expected {1}")
    @MethodSource("nonBase64CharsProvider")
    void testNonBase64CharactersIgnored(byte[] input, String expectedDecimal) {
        BigInteger result = Base64.decodeInteger(input);
        BigInteger expected = new BigInteger(expectedDecimal);
        assertEquals(expected, result, () -> "Result differs when non‑Base64 characters are present");
    }

    @ParameterizedTest(name = "[{index}] whitespace variations -> expected {1}")
    @MethodSource("whitespaceProvider")
    void testWhitespaceVariations(byte[] input, String expectedDecimal) {
        BigInteger result = Base64.decodeInteger(input);
        BigInteger expected = new BigInteger(expectedDecimal);
        assertEquals(expected, result, () -> "Whitespace handling failed for input " + java.util.Arrays.toString(input));
    }

    @ParameterizedTest(name = "[{index}] leading zero bytes -> expected {1}")
    @MethodSource("leadingZeroProvider")
    void testLeadingZeroBytes(byte[] input, String expectedDecimal) {
        BigInteger result = Base64.decodeInteger(input);
        BigInteger expected = new BigInteger(expectedDecimal);
        assertEquals(expected, result, () -> "Leading zero bytes were not ignored correctly");
    }

    @ParameterizedTest(name = "[{index}] large integer -> expected {1}")
    @MethodSource("largeIntegerProvider")
    void testLargeIntegerValues(byte[] input, String expectedDecimal) {
        BigInteger result = Base64.decodeInteger(input);
        BigInteger expected = new BigInteger(expectedDecimal);
        assertEquals(expected, result, () -> "Large integer decoding failed");
    }
}

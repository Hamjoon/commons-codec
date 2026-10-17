package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

class Base64_encodeBase64_4_Test {

    /**
     * Sample data used by several tests
     */
    private static final byte[] SAMPLE = "Apache Commons Codec".getBytes();

    private static Stream<Arguments> provideEncodeBase64Cases() {
        return Stream.of(// Empty input (already covered by a dedicated test, but kept here for completeness)
        Arguments.of(new byte[0], false, false, 10, new byte[0], "Empty input should return empty array"), // Standard encoding of "any"
        Arguments.of(new byte[] { 97, 110, 121 }, false, false, 10, "YW55".getBytes(StandardCharsets.US_ASCII), "Standard encoding of 'any'"), // URL‑safe encoding of [0xFB,0xFF,0xFE]
        Arguments.of(new byte[] { (byte) 0xFB, (byte) 0xFF, (byte) 0xFE }, false, true, 10, "-__-".getBytes(StandardCharsets.US_ASCII), "URL‑safe encoding of three non‑ASCII bytes"), // Max size exact fit
        Arguments.of(new byte[] { 1, 2, 3, 4 }, false, false, 8, "AQIDBA==".getBytes(StandardCharsets.US_ASCII), "Exact maxResultSize should succeed"), // Max size too small
        Arguments.of(new byte[] { 1, 2, 3, 4 }, false, false, 7, IllegalArgumentException.class, "Too small maxResultSize must throw"));
    }

    /**
     * The method should return the original array when the input is empty.
     * This also verifies that the reference is unchanged (not a copy).
     */
    @Test
    void encodeBase64_EmptyInput_ReturnsSameArray() {
        byte[] empty = new byte[0];
        byte[] result = Base64.encodeBase64(empty, false, false, Integer.MAX_VALUE);
        assertSame(empty, result, "Empty input must be returned unchanged");
    }

    /**
     * Verify normal (non‑chunked, non‑URL‑safe) encoding produces the same
     * bytes as the simple overload {@code Base64.encodeBase64(byte[])}.
     */
    @Test
    void encodeBase64_NonChunkedNonUrlSafe_MatchesSimpleOverload() {
        // simple overload
        byte[] expected = Base64.encodeBase64(SAMPLE);
        byte[] actual = Base64.encodeBase64(SAMPLE, false, false, Integer.MAX_VALUE);
        assertArrayEquals(expected, actual, "Encoding without chunking or URL‑safe must match simple overload");
    }

    /**
     * When URL‑safe is requested the result must be identical to the dedicated
     * {@code encodeBase64URLSafe} method.
     */
    @Test
    void encodeBase64_UrlSafe_MatchesUrlSafeOverload() {
        byte[] expected = Base64.encodeBase64URLSafe(SAMPLE);
        byte[] actual = Base64.encodeBase64(SAMPLE, false, true, Integer.MAX_VALUE);
        assertArrayEquals(expected, actual, "URL‑safe encoding must match encodeBase64URLSafe");
    }

    /**
     * Chunked encoding adds line separators only when the encoded output exceeds
     * the default line length (76). For a short payload the result must be identical
     * to the non‑chunked version.
     */
    @Test
    void encodeBase64_Chunked_ShortInput_NoSeparatorAdded() {
        // non‑chunked
        byte[] expected = Base64.encodeBase64(SAMPLE);
        byte[] actual = Base64.encodeBase64(SAMPLE, true, false, Integer.MAX_VALUE);
        assertArrayEquals(expected, actual, "Chunked encoding of short data must not insert line separators");
    }

    /**
     * Verify that the method respects {@code maxResultSize}. The encoded length is
     * obtained from a fresh {@code Base64} instance; the test forces an
     * {@code IllegalArgumentException} when the limit is smaller than that length.
     */
    @Test
    void encodeBase64_MaxResultSize_TooSmall_ThrowsException() {
        // Build a relatively large input so that the encoded length is definitely > 10
        byte[] large = new byte[256];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) i;
        }
        // Compute the length that the encoder would produce
        // default configuration
        Base64 encoder = new Base64();
        long encodedLen = encoder.getEncodedLength(large);
        // Use a maxResultSize that is smaller than the required length
        long tooSmall = encodedLen - 1;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Base64.encodeBase64(large, false, false, (int) tooSmall), "Expected IllegalArgumentException when maxResultSize is too small");
        assertTrue(ex.getMessage().contains("Input array too big"), "Exception message should indicate the input is too big");
    }

    /**
     * Null input should be treated as empty and returned unchanged (i.e. {@code null}).
     */
    @Test
    void encodeBase64_NullInput_ReturnsNull() {
        byte[] result = Base64.encodeBase64(null, false, false, 10);
        assertNull(result, "Null input must result in null output");
    }

    /**
     * Standard encoding of a short ASCII payload without chunking or URL‑safe mode.
     * Expected Base64 string: "YW55".
     */
    @Test
    void encodeBase64_StandardEncoding_ShortPayload() {
        // "any"
        byte[] input = new byte[] { 97, 110, 121 };
        byte[] expected = "YW55".getBytes(StandardCharsets.US_ASCII);
        byte[] actual = Base64.encodeBase64(input, false, false, 10);
        assertArrayEquals(expected, actual, "Standard encoding should match expected Base64 string");
    }

    /**
     * Chunked encoding of 57 bytes of 'a' (ASCII 97). The output length should be
     * 78 bytes: 76 Base64 characters + CRLF ("\r\n").
     */
    @Test
    void encodeBase64_ChunkedEncoding_InsertsCRLF() {
        // 57 times the character 'a'
        byte[] input = new byte[57];
        for (int i = 0; i < input.length; i++) {
            input[i] = 97;
        }
        byte[] result = Base64.encodeBase64(input, true, false, 2000);
        // Expected length = 76 (base64) + 2 (CRLF) = 78
        assertEquals(78, result.length, "Chunked output should contain an extra CRLF");
        // Verify that the CRLF appears exactly after the 76th character
        assertEquals((byte) '\r', result[76], "CR character should be at position 76");
        assertEquals((byte) '\n', result[77], "LF character should be at position 77");
    }

    /**
     * URL‑safe encoding of three non‑ASCII bytes. Expected Base64 URL‑safe string: "-__-".
     */
    @Test
    void encodeBase64_UrlSafeEncoding_NonAsciiBytes() {
        byte[] input = new byte[] { (byte) 0xFB, (byte) 0xFF, (byte) 0xFE };
        byte[] expected = "-__-".getBytes(StandardCharsets.US_ASCII);
        byte[] actual = Base64.encodeBase64(input, false, true, 10);
        assertArrayEquals(expected, actual, "URL‑safe encoding should replace '+' and '/' with '-' and '_'");
    }

    /**
     * Encoding succeeds when {@code maxResultSize} exactly matches the required output size.
     * Input: [1,2,3,4] → Base64 "AQIDBA==" (8 bytes).
     */
    @Test
    void encodeBase64_MaxResultSize_ExactFit() {
        byte[] input = new byte[] { 1, 2, 3, 4 };
        byte[] expected = "AQIDBA==".getBytes(StandardCharsets.US_ASCII);
        byte[] actual = Base64.encodeBase64(input, false, false, 8);
        assertArrayEquals(expected, actual, "Encoding should succeed when maxResultSize equals required length");
    }

    /**
     * Encoding fails when {@code maxResultSize} is smaller than the required output size.
     * Input: [1,2,3,4] → required length 8, limit = 7.
     */
    @Test
    void encodeBase64_MaxResultSize_TooSmall_Throws() {
        byte[] input = new byte[] { 1, 2, 3, 4 };
        assertThrows(IllegalArgumentException.class, () -> Base64.encodeBase64(input, false, false, 7), "Should throw IllegalArgumentException when maxResultSize is insufficient");
    }

    /**
     * Negative {@code maxResultSize} should always cause an exception for non‑empty input.
     */
    @Test
    void encodeBase64_NegativeMaxResultSize_Throws() {
        byte[] input = new byte[] { 10, 20, 30 };
        assertThrows(IllegalArgumentException.class, () -> Base64.encodeBase64(input, false, false, -1), "Negative maxResultSize must trigger IllegalArgumentException");
    }

    /**
     * Zero {@code maxResultSize} cannot accommodate any output; any non‑empty input must fail.
     */
    @Test
    void encodeBase64_ZeroMaxResultSize_Throws() {
        byte[] input = new byte[] { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> Base64.encodeBase64(input, false, false, 0), "Zero maxResultSize must trigger IllegalArgumentException for non‑empty input");
    }

    /**
     * Chunked encoding increases the output size because of line separators.
     * Providing a too‑small {@code maxResultSize} (70) must cause an exception.
     */
    @Test
    void encodeBase64_MaxResultSize_ChunkedTooSmall_Throws() {
        // 57 'a' bytes → 78 bytes when chunked (including CRLF)
        byte[] input = new byte[57];
        for (int i = 0; i < input.length; i++) {
            input[i] = 97;
        }
        assertThrows(IllegalArgumentException.class, () -> Base64.encodeBase64(input, true, false, 70), "Chunked output exceeding maxResultSize must throw IllegalArgumentException");
    }

    /**
     * Parameterized test covering a few representative cases.
     * Each argument supplies: binaryData, isChunked, urlSafe, maxResultSize,
     * expectedResult (as a String or special marker), and a description.
     */
    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("provideEncodeBase64Cases")
    void encodeBase64_Parameterized(byte[] binaryData, boolean isChunked, boolean urlSafe, int maxResultSize, // either byte[] or Class<? extends Throwable>
    Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> Base64.encodeBase64(binaryData, isChunked, urlSafe, maxResultSize), description);
        } else {
            // Expected a byte[] result
            byte[] expectedBytes = (byte[]) expected;
            byte[] actual = Base64.encodeBase64(binaryData, isChunked, urlSafe, maxResultSize);
            assertArrayEquals(expectedBytes, actual, description);
        }
    }
}

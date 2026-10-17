package org.apache.commons.codec.binary;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64_2_Test {

    static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(// null input → null output
        Arguments.of("null input, non‑chunked", null, false, null), // empty array → empty array
        Arguments.of("empty input, non‑chunked", new byte[0], false, new byte[0]));
    }

    static Stream<Arguments> basicEncodingProvider() {
        return Stream.of(Arguments.of("encode \"any\"", toByteArray(97, 110, 121), false, "YW55"), Arguments.of("encode three zero bytes", toByteArray(0, 0, 0), false, "AAAA"));
    }

    static Stream<Arguments> paddingProvider() {
        return Stream.of(Arguments.of("single zero byte → AA==", toByteArray(0), false, "AA=="), Arguments.of("two bytes 0,1 → AAE=", toByteArray(0, 1), false, "AAE="), Arguments.of("four bytes 1,2,3,4 → AQIDBA==", toByteArray(1, 2, 3, 4), false, "AQIDBA=="));
    }

    static Stream<Arguments> chunkedProvider() {
        return Stream.of(// >76‑character payload – one CRLF at the 76‑char boundary
        Arguments.of("long sentence with chunking", toByteArray(84, 104, 105, 115, 32, 105, 115, 32, 97, 32, 108, 111, 110, 103, 101, 114, 32, 115, 116, 114, 105, 110, 103, 32, 116, 111, 32, 116, 101, 115, 116, 32, 116, 104, 101, 32, 99, 104, 117, 110, 107, 105, 110, 103, 32, 108, 111, 103, 105, 99, 32, 102, 101, 97, 116, 117, 114, 101, 115, 46, 32, 73, 116, 32, 99, 111, 110, 116, 97, 105, 110, 115, 32, 109, 111, 114, 101, 32, 116, 104, 97, 110, 76, 69, 78, 71, 84, 72, 32, 99, 104, 97, 114, 115, 46), true, // Expected string ends with CRLF because the encoded length is a multiple of 76
        "VGhpcyBpcyBhIGxvbmdlciBzdHJpbmcgdG8gdGVzdCB0aGUgY2h1bmtpbmcgbG9naWMgZmVhdHVyZXMuIEl0IGNvbnRhaW5zIG1vcmUgdGhhbkxFTkdUSENoYXJzLg==\r\n"), // Short data – no line break should be added
        Arguments.of("three bytes with chunking (no break)", toByteArray(1, 2, 3), true, "AQID"));
    }

    static Stream<Arguments> additionalPaddingProvider() {
        return Stream.of(Arguments.of("five bytes → AQIDBAU=", toByteArray(1, 2, 3, 4, 5), false, "AQIDBAU="));
    }

    static Stream<Arguments> chunkedBoundaryProvider() {
        // 57 bytes → 76 Base64 chars, no CRLF
        byte[] exactly57 = new byte[57];
        // 0x61
        Arrays.fill(exactly57, (byte) 'a');
        // 58 bytes → 76 chars + encoding of 1 extra byte, should have CRLF after first 76 chars
        byte[] fiftyEight = Arrays.copyOf(exactly57, 58);
        fiftyEight[57] = (byte) 'a';
        return Stream.of(Arguments.of("57 bytes (exactly one 76‑char line)", exactly57, true, // 57 'a' bytes produce 76 Base64 chars, no line break
        "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFh"), Arguments.of("58 bytes (requires line break)", fiftyEight, true, // First 76 chars then CRLF then the encoding of the remaining single byte ("YQ==")
        "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFh\r\nYQ=="));
    }

    /* -------------------------------------------------------------
     *  Helper – converts int varargs to a byte[] respecting unsigned values
     * ------------------------------------------------------------- */
    private static byte[] toByteArray(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) (values[i] & 0xFF);
        }
        return bytes;
    }

    @Test
    void encodeBase64_NullInput_ReturnsNull() {
        assertNull(Base64.encodeBase64(null, false), "Encoding a null byte array should return null");
    }

    @Test
    void encodeBase64_EmptyInput_ReturnsEmptyArray() {
        byte[] empty = new byte[0];
        byte[] result = Base64.encodeBase64(empty, false);
        assertNotNull(result, "Result must not be null for empty input");
        assertEquals(0, result.length, "Result array length must be zero for empty input");
    }

    @Test
    void encodeBase64_NonChunked_MatchesThreeArgOverload() {
        byte[] data = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        // Expected result using the three‑argument overload (urlSafe = false)
        byte[] expected = Base64.encodeBase64(data, false, false);
        byte[] actual = Base64.encodeBase64(data, false);
        assertArrayEquals(expected, actual, "Non‑chunked encoding should match the three‑argument overload");
    }

    @Test
    void encodeBase64_Chunked_InsertsLineSeparatorsAndMatchesThreeArgOverload() {
        // Create a payload large enough to require more than one line when chunked.
        // 120 bytes → 160 Base64 characters → two line breaks (after 76 chars).
        byte[] data = new byte[120];
        new Random(0x5a5a5a5a).nextBytes(data);
        // Expected result using the three‑argument overload (urlSafe = false)
        byte[] expected = Base64.encodeBase64(data, true, false);
        byte[] actual = Base64.encodeBase64(data, true);
        assertArrayEquals(expected, actual, "Chunked encoding should match the three‑argument overload");
        // Verify that the default CRLF line separator is present.
        String encodedString = new String(actual, StandardCharsets.US_ASCII);
        assertTrue(encodedString.contains("\r\n"), "Chunked output must contain the CRLF line separator");
        // Ensure that line breaks occur at the correct interval (every 76 characters).
        // Split on CRLF and verify each line length (except possibly the last).
        String[] lines = encodedString.split("\r\n");
        for (int i = 0; i < lines.length - 1; i++) {
            assertEquals(76, lines[i].length(), "Each chunked line (except the last) must be exactly 76 characters long");
        }
    }

    /* -------------------------------------------------------------
     *  Null and Empty Input
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] null/empty – {0}")
    @MethodSource("nullAndEmptyProvider")
    @DisplayName("Null and Empty Input")
    void encodeBase64_NullAndEmpty(String description, byte[] input, boolean isChunked, Object expected) {
        byte[] result = Base64.encodeBase64(input, isChunked);
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertEquals(((byte[]) expected).length, result.length, description);
            assertArrayEquals((byte[]) expected, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Basic Encoding (no chunking)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] basic – {0}")
    @MethodSource("basicEncodingProvider")
    @DisplayName("Basic Encoding (no chunking)")
    void encodeBase64_Basic(String description, byte[] input, boolean isChunked, String expectedBase64) {
        byte[] actual = Base64.encodeBase64(input, isChunked);
        assertNotNull(actual, description);
        String actualStr = new String(actual, StandardCharsets.US_ASCII);
        assertEquals(expectedBase64, actualStr, description);
    }

    /* -------------------------------------------------------------
     *  Padding Cases (no chunking)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] padding – {0}")
    @MethodSource("paddingProvider")
    @DisplayName("Padding Cases (no chunking)")
    void encodeBase64_Padding(String description, byte[] input, boolean isChunked, String expectedBase64) {
        byte[] actual = Base64.encodeBase64(input, isChunked);
        String actualStr = new String(actual, StandardCharsets.US_ASCII);
        assertEquals(expectedBase64, actualStr, description);
    }

    /* -------------------------------------------------------------
     *  Chunked Encoding (line breaks every 76 characters)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] chunked – {0}")
    @MethodSource("chunkedProvider")
    @DisplayName("Chunked Encoding (line breaks every 76 characters)")
    void encodeBase64_Chunked(String description, byte[] input, boolean isChunked, String expectedBase64) {
        byte[] actual = Base64.encodeBase64(input, isChunked);
        String actualStr = new String(actual, StandardCharsets.US_ASCII);
        assertEquals(expectedBase64, actualStr, description);
        // Additional verification for line‑separator logic when a CRLF is expected
        if (expectedBase64.contains("\r\n")) {
            // Ensure a CRLF exists and that each line (except possibly the last) is exactly 76 chars
            String[] lines = actualStr.split("\r\n");
            for (int i = 0; i < lines.length - 1; i++) {
                assertEquals(76, lines[i].length(), "Line " + (i + 1) + " must be 76 characters long");
            }
        } else {
            // When no CRLF is expected, confirm that the output does not contain it
            assertFalse(actualStr.contains("\r\n"), "Output must not contain CRLF when length < 76");
        }
    }

    /* -------------------------------------------------------------
     *  Additional Padding Cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] additional padding – {0}")
    @MethodSource("additionalPaddingProvider")
    @DisplayName("Additional Padding Cases")
    void encodeBase64_AdditionalPadding(String description, byte[] input, boolean isChunked, String expectedBase64) {
        byte[] actual = Base64.encodeBase64(input, isChunked);
        String actualStr = new String(actual, StandardCharsets.US_ASCII);
        assertEquals(expectedBase64, actualStr, description);
    }

    /* -------------------------------------------------------------
     *  Chunked Boundary Cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] chunked boundary – {0}")
    @MethodSource("chunkedBoundaryProvider")
    @DisplayName("Chunked Boundary Cases")
    void encodeBase64_ChunkedBoundary(String description, byte[] input, boolean isChunked, String expectedBase64) {
        byte[] actual = Base64.encodeBase64(input, isChunked);
        String actualStr = new String(actual, StandardCharsets.US_ASCII);
        assertEquals(expectedBase64, actualStr, description);
    }

    /* -------------------------------------------------------------
     *  Non‑ASCII Encoding
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Non‑ASCII bytes (values > 127)")
    void encodeBase64_NonAscii() {
        byte[] input = toByteArray(0xFF, 0xEE, 0xDD);
        byte[] actual = Base64.encodeBase64(input, false);
        String result = new String(actual, StandardCharsets.US_ASCII);
        assertEquals("/+7d", result, "Encoding of bytes [255,238,221] must be '/+7d'");
    }

    /* -------------------------------------------------------------
     *  Chunking with Single Byte
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Chunking flag true but single‑byte payload")
    void encodeBase64_ChunkedSingleByte() {
        byte[] input = toByteArray(0xFF);
        byte[] actual = Base64.encodeBase64(input, true);
        String result = new String(actual, StandardCharsets.US_ASCII);
        assertEquals("/w==", result, "Single‑byte with chunking must equal non‑chunked output");
        assertFalse(result.contains("\r\n"), "No CRLF should be present for <76‑char output");
    }

    /* -------------------------------------------------------------
     *  Immutability – returned array must be a new instance
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Returned array is a new instance (immutability)")
    void encodeBase64_ReturnsNewArrayInstance() {
        byte[] input = toByteArray(1, 2, 3);
        byte[] result = Base64.encodeBase64(input, false);
        assertNotSame(input, result, "Method must not return the original input array");
        // Verify content is correct using the three‑argument overload as the source of truth
        byte[] expected = Base64.encodeBase64(input, false, false);
        assertArrayEquals(expected, result, "Returned array must contain correct Base64 bytes");
    }
}

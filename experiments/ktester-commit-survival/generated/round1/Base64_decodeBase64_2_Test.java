package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base64_decodeBase64_2_Test{

    /* --------------------------------------------------------------------- */
    /*  Lifecycle methods – they are intentionally empty for this test suite */
    /* --------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Base64.decodeBase64(String)                               */
    /* --------------------------------------------------------------------- */

    @Test
    void decodeBase64_validString_returnsOriginalBytes() {
        // Given a plain text string
        final String original = "Apache Commons Codec – Base64 test";
        final byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);

        // When it is encoded using the library under test
        final String encoded = Base64.encodeBase64String(originalBytes);

        // And decoded back using the static method under test
        final byte[] decoded = Base64.decodeBase64(encoded);

        // Then the decoded bytes must match the original bytes
        assertArrayEquals(originalBytes, decoded,
                () -> "Decoded bytes differ from original for encoded value: " + encoded);
    }

    @Test
    void decodeBase64_nullInput_returnsNull() {
        // Given a null reference
        final String input = null;

        // When decodeBase64 is invoked
        final byte[] result = Base64.decodeBase64(input);

        // Then the result should be null (the library treats null as a no‑op)
        assertNull(result, "decodeBase64(null) should return null");
    }

    @Test
    void decodeBase64_emptyString_returnsEmptyArray() {
        // Given an empty string
        final String input = "";

        // When decoded
        final byte[] result = Base64.decodeBase64(input);

        // Then the result must be a zero‑length array (not null)
        assertNotNull(result, "Result must not be null for empty input");
        assertEquals(0, result.length, "Result array length should be 0 for empty input");
    }

    @Test
    void decodeBase64_stringWithInvalidCharacters_isLenient() {
        // Given a string that contains characters outside the Base64 alphabet
        // The default Base64 instance is lenient and simply ignores them.
        final String input = "!!@@##$$%%^^&&**(()))__++=="; // no valid Base64 chars

        // When decoded
        final byte[] result = Base64.decodeBase64(input);

        // Then the decoder should return an empty array (no data could be extracted)
        assertNotNull(result, "Result must not be null for garbage input");
        assertEquals(0, result.length, "Result should be empty when input contains no valid Base64 chars");
    }

    @Test
    void decodeBase64_stringWithLineBreaks_isHandledCorrectly() {
        // Given a Base64 string that contains CRLF and whitespace characters
        final String plain = "Line break handling in Base64 decoding.";
        final byte[] plainBytes = plain.getBytes(StandardCharsets.UTF_8);
        final String encoded = Base64.encodeBase64String(plainBytes);

        // Insert line breaks every 10 characters
        final StringBuilder broken = new StringBuilder();
        for (int i = 0; i < encoded.length(); i++) {
            broken.append(encoded.charAt(i));
            if ((i + 1) % 10 == 0) {
                broken.append("\r\n");
            }
        }

        // When decoded
        final byte[] decoded = Base64.decodeBase64(broken.toString());

        // Then the original bytes must be recovered
        assertArrayEquals(plainBytes, decoded,
                () -> "Decoded bytes differ when input contains line breaks");
    }

    @Test
    void decodeBase64_urlSafeString_isDecodedCorrectly() {
        // Given a URL‑safe Base64 representation (uses '-' and '_' instead of '+' and '/')
        final String original = "URL‑safe Base64 test 🚀";
        final byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);

        // Encode using the URL‑safe helper
        final String urlSafeEncoded = Base64.encodeBase64URLSafeString(originalBytes);

        // When decoded with the regular decodeBase64 method
        final byte[] decoded = Base64.decodeBase64(urlSafeEncoded);

        // Then the bytes must match the original
        assertArrayEquals(originalBytes, decoded,
                () -> "URL‑safe encoded string was not decoded correctly");
    }

    @Test
    void decodeBase64_stringWithPadding_isProcessedCorrectly() {
        // Given a string that ends with padding characters '='
        final String original = "Padding test";
        final byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);
        final String encodedWithPadding = Base64.encodeBase64String(originalBytes); // includes '=' padding

        // When decoded
        final byte[] decoded = Base64.decodeBase64(encodedWithPadding);

        // Then the result must equal the original bytes
        assertArrayEquals(originalBytes, decoded,
                () -> "Decoded bytes do not match original when input contains padding");
    }

    @Test
    void decodeBase64_stringWithMixedStandardAndUrlSafeChars_isHandledLeniently() {
        // Create a mixed string: standard Base64 chars plus URL‑safe replacements
        final String mixed = "ABCDabcd0123+/_-"; // '_' and '-' are URL‑safe variants
        // The library's lenient decoder should treat '_' as '/' and '-' as '+'
        // Expect the same bytes as decoding the standard version "ABCDabcd0123+/+/"
        final String standardEquivalent = "ABCDabcd0123+/+/";

        final byte[] expected = Base64.decodeBase64(standardEquivalent);
        final byte[] actual = Base64.decodeBase64(mixed);

        assertArrayEquals(expected, actual,
                () -> "Mixed standard/URL‑safe characters were not decoded as expected");
    }
}
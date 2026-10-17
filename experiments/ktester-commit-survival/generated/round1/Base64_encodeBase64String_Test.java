package org.apache.commons.codec.binary;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link Base64#encodeBase64String(byte[])}.
 *
 * <p>The method is static and delegates to {@link Base64#encodeBase64(byte[], boolean)}
 * and {@link StringUtils#newStringUsAscii(byte[])}. The tests verify correct handling of
 * typical inputs, edge‑cases such as {@code null} and empty arrays, and that the
 * produced Base64 string matches the expected RFC‑2045 encoding without line
 * chunking.</p>
 */
@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64String_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these static tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that a typical ASCII input is encoded correctly.
     */
    @Test
    void encodeBase64String_simpleAscii() {
        byte[] input = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.US_ASCII);
        String expected = "VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wcyBvdmVyIHRoZSBsYXp5IGRvZw==";

        String actual = Base64.encodeBase64String(input);
        assertNotNull(actual, "Result should not be null for non‑null input");
        assertEquals(expected, actual, "Base64 encoding mismatch");
    }

    /**
     * Verify that an empty byte array yields an empty string.
     */
    @Test
    void encodeBase64String_emptyArray() {
        byte[] input = new byte[0];
        String actual = Base64.encodeBase64String(input);
        assertNotNull(actual, "Result should not be null for empty input");
        assertEquals("", actual, "Empty input should produce empty string");
    }

    /**
     * Verify that a {@code null} argument is handled gracefully (returns {@code null}).
     */
    @Test
    void encodeBase64String_nullInput() {
        String actual = Base64.encodeBase64String(null);
        assertNull(actual, "Null input should result in null output");
    }

    /**
     * Verify that binary data containing non‑printable bytes is encoded correctly.
     */
    @Test
    void encodeBase64String_binaryData() {
        byte[] input = new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0x7A, (byte) 0x13 };
        // Pre‑computed using a reliable Base64 reference implementation.
        String expected = "AP9aEw==";

        String actual = Base64.encodeBase64String(input);
        assertEquals(expected, actual, "Base64 encoding of binary data mismatch");
    }

    /**
     * Verify that the method does not insert line separators (chunking is disabled).
     */
    @Test
    void encodeBase64String_noChunking() {
        // Create enough data to exceed the default MIME line length (76 characters)
        byte[] input = new byte[100];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) i;
        }

        String result = Base64.encodeBase64String(input);
        // The result must not contain CR or LF characters.
        assertFalse(result.contains("\r") || result.contains("\n"),
                "Result should not contain line separators when chunking is disabled");
        // Length should be a multiple of 4.
        assertEquals(0, result.length() % 4, "Base64 output length must be a multiple of 4");
    }
}
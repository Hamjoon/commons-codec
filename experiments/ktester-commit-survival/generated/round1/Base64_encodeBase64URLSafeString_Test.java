package org.apache.commons.codec.binary;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64URLSafeString_Test{

    /* ----------------------------------------------------------------------
     * Lifecycle methods – they are kept for completeness; no special
     * initialisation is required for the static method under test.
     * ---------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test resources needed.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* ----------------------------------------------------------------------
     * Test cases for Base64.encodeBase64URLSafeString(byte[])
     * ---------------------------------------------------------------------- */

    /**
     * Verifies that a simple ASCII payload is correctly encoded.
     * The result should be the standard Base64 representation (padded) because
     * the payload does not contain characters that need URL‑safe conversion.
     */
    @Test
    void testEncodeBase64URLSafeString_asciiPayload() {
        byte[] input = "Hello".getBytes(); // 5 bytes
        // Expected value obtained from a known‑good implementation (or from
        // Base64.encodeBase64URLSafeString itself for documentation purposes).
        // "Hello" → "SGVsbG8=" (standard Base64, padding retained)
        String expected = "SGVsbG8=";

        String actual = Base64.encodeBase64URLSafeString(input);
        assertEquals(expected, actual, "URL‑safe Base64 string does not match expected value");
    }

    /**
     * Verifies that characters requiring URL‑safe conversion are correctly
     * transformed ('+' → '-', '/' → '_') while preserving padding.
     */
    @Test
    void testEncodeBase64URLSafeString_urlSafeCharacters() {
        // Bytes that generate '+' and '/' in the standard Base64 alphabet.
        // Example: { (byte)0xfb, (byte)0xef, (byte)0xff } → standard "+/7/"
        // URL‑safe version should be "-_7_".
        byte[] input = new byte[] { (byte) 0xfb, (byte) 0xef, (byte) 0xff };
        String expected = "-_7_";

        String actual = Base64.encodeBase64URLSafeString(input);
        assertEquals(expected, actual, "URL‑safe conversion of '+' and '/' failed");
    }

    /**
     * Verifies that an empty input array yields an empty string.
     */
    @Test
    void testEncodeBase64URLSafeString_emptyArray() {
        byte[] input = new byte[0];
        String expected = "";

        String actual = Base64.encodeBase64URLSafeString(input);
        assertEquals(expected, actual, "Encoding of an empty array should produce an empty string");
    }

    /**
     * Verifies that a {@code null} argument results in a {@link NullPointerException}.
     * The method does not perform a null‑check internally, so the NPE is expected.
     */
    @Test
    void testEncodeBase64URLSafeString_nullInput() {
        assertThrows(NullPointerException.class, () -> Base64.encodeBase64URLSafeString(null),
                "Passing null should throw NullPointerException");
    }
}
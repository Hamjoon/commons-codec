package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Base64#isInAlphabet(byte)}.
 *
 * The method is {@code protected}, but because this test class Base64_isInAlphabet_Test* package it can invoke it directly on a {@link Base64} instance.
 */
class Base64_isInAlphabet_Test {

    /** Instance using the default (standard) Base64 alphabet. */
    private Base64 standardBase64;

    /** Instance using URL‑safe Base64 (different encode table, same decode table). */
    private Base64 urlSafeBase64;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        standardBase64 = new Base64();                     // standard alphabet
        urlSafeBase64 = new Base64(true);                  // URL‑safe mode
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly necessary but keeps the pattern consistent.
        standardBase64 = null;
        urlSafeBase64 = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /* --------------------------------------------------------------------- */
    /* Positive test cases – bytes that belong to the Base64 alphabet.      */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Standard Base64: alphabet characters return true")
    void testIsInAlphabet_StandardAlphabet_ValidBytes() {
        // Upper‑case letters
        assertTrue(standardBase64.isInAlphabet((byte) 'A'));
        assertTrue(standardBase64.isInAlphabet((byte) 'Z'));

        // Lower‑case letters
        assertTrue(standardBase64.isInAlphabet((byte) 'a'));
        assertTrue(standardBase64.isInAlphabet((byte) 'z'));

        // Digits
        assertTrue(standardBase64.isInAlphabet((byte) '0'));
        assertTrue(standardBase64.isInAlphabet((byte) '9'));

        // Symbols used by the standard alphabet
        assertTrue(standardBase64.isInAlphabet((byte) '+'));
        assertTrue(standardBase64.isInAlphabet((byte) '/'));
    }

    @Test
    @DisplayName("URL‑safe Base64: alphabet characters return true")
    void testIsInAlphabet_UrlSafeAlphabet_ValidBytes() {
        // The URL‑safe alphabet replaces '+' with '-' and '/' with '_' .
        assertTrue(urlSafeBase64.isInAlphabet((byte) '-'));
        assertTrue(urlSafeBase64.isInAlphabet((byte) '_'));

        // All other characters are identical to the standard alphabet.
        assertTrue(urlSafeBase64.isInAlphabet((byte) 'A'));
        assertTrue(urlSafeBase64.isInAlphabet((byte) 'z'));
        assertTrue(urlSafeBase64.isInAlphabet((byte) '5'));
    }

    /* --------------------------------------------------------------------- */
    /* Negative test cases – bytes that are NOT part of the Base64 alphabet. */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Standard Base64: byte outside the decode table range returns false")
    void testIsInAlphabet_StandardAlphabet_ByteOutOfRange() {
        // The method checks (octet >= 0); casting a negative value yields a
        // negative signed byte, which should cause the method to return false.
        assertFalse(standardBase64.isInAlphabet((byte) -1));

        // Positive value that is not in the alphabet (e.g., ':' = 58)
        assertFalse(standardBase64.isInAlphabet((byte) ':'));

        // Highest positive byte value (127) – not part of the alphabet.
        assertFalse(standardBase64.isInAlphabet((byte) 127));
    }

    @Test
    @DisplayName("URL‑safe Base64: byte not in alphabet returns false")
    void testIsInAlphabet_UrlSafeAlphabet_InvalidBytes() {
        // Padding character '=' is not part of the alphabet.
        assertFalse(urlSafeBase64.isInAlphabet((byte) '='));

        // A character that belongs to the standard alphabet but not to URL‑safe
        // (e.g., '+') should also be rejected.
        assertFalse(urlSafeBase64.isInAlphabet((byte) '+'));

        // Random non‑alphabetic character.
        assertFalse(urlSafeBase64.isInAlphabet((byte) '@'));
    }
}
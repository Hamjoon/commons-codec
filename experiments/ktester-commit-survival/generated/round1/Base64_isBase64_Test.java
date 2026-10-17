package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class Base64_isBase64_Test{

    // No external resources or dependencies are required for the static method under test.

    @BeforeAll
    static void setupBeforeAll() {
        // Nothing to set up globally.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to set up before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * All characters defined in the standard Base64 alphabet must be recognised as Base64.
     */
    @Test
    void testStandardAlphabetCharactersAreBase64() {
        final String standardAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (char c : standardAlphabet.toCharArray()) {
            byte b = (byte) c;
            assertTrue(Base64.isBase64(b),
                    () -> "Expected character '" + c + "' (0x" + Integer.toHexString(b & 0xFF) + ") to be Base64");
        }
    }

    /**
     * URL‑safe characters '_' and '-' are also accepted by the decoder.
     */
    @Test
    void testUrlSafeCharactersAreBase64() {
        byte underscore = (byte) '_'; // 0x5F
        byte dash = (byte) '-';       // 0x2D
        assertTrue(Base64.isBase64(underscore), "Underscore '_' should be recognised as Base64");
        assertTrue(Base64.isBase64(dash), "Dash '-' should be recognised as Base64");
    }

    /**
     * The padding character '=' must be recognised as Base64.
     */
    @Test
    void testPaddingCharacterIsBase64() {
        byte pad = (byte) '=';
        assertTrue(Base64.isBase64(pad), "Padding character '=' should be recognised as Base64");
    }

    /**
     * Whitespace characters are *not* considered Base64 by the {@code isBase64(byte)} method.
     * (Whitespace handling is performed by the {@code isBase64(byte[])} overload.)
     */
    @Test
    void testWhitespaceCharactersAreNotBase64() {
        byte space = (byte) ' ';
        byte tab = (byte) '\t';
        byte newline = (byte) '\n';
        byte carriageReturn = (byte) '\r';

        assertFalse(Base64.isBase64(space), "Space should not be recognised as Base64");
        assertFalse(Base64.isBase64(tab), "Tab should not be recognised as Base64");
        assertFalse(Base64.isBase64(newline), "Newline should not be recognised as Base64");
        assertFalse(Base64.isBase64(carriageReturn), "Carriage return should not be recognised as Base64");
    }

    /**
     * Characters that are not part of the Base64 alphabet and are not padding must be rejected.
     */
    @Test
    void testInvalidCharactersAreNotBase64() {
        char[] invalid = {'@', '#', '$', '%', '^', '&', '*', '(', ')', '=', // '=' is padding, already tested separately
                         '`', '~', '[', ']', '{', '}', '|', '\\', '"', '\''};

        for (char c : invalid) {
            // Skip the padding character which is valid; we already have a dedicated test.
            if (c == '=') {
                continue;
            }
            byte b = (byte) c;
            assertFalse(Base64.isBase64(b),
                    () -> "Expected character '" + c + "' (0x" + Integer.toHexString(b & 0xFF) + ") to be NOT Base64");
        }
    }

    /**
     * Verify that values outside the unsigned byte range (negative values) are not considered Base64.
     */
    @Test
    void testNegativeByteValuesAreNotBase64() {
        byte[] negatives = {(byte) -128, (byte) -1, (byte) -64};
        for (byte b : negatives) {
            assertFalse(Base64.isBase64(b),
                    () -> "Negative byte value 0x" + Integer.toHexString(b & 0xFF) + " should not be Base64");
        }
    }

    /**
     * Verify that values beyond the ASCII range (>= 128) are not considered Base64.
     */
    @Test
    void testHighByteValuesAreNotBase64() {
        byte[] highValues = {(byte) 127, (byte) -128}; // 127 is the highest positive byte, -128 wraps to 128 unsigned
        for (byte b : highValues) {
            // 127 is not a Base64 character; -128 (0x80) is also outside the table.
            assertFalse(Base64.isBase64(b),
                    () -> "High byte value 0x" + Integer.toHexString(b & 0xFF) + " should not be Base64");
        }
    }

    /**
     * The {@code isBase64(byte[])} overload should ignore whitespace and return true only if
     * every non‑whitespace byte is a valid Base64 character.
     */
    @Test
    void testIsBase64ArrayIgnoresWhitespace() {
        byte[] mixed = ("A B\tC\nD\rE" + // valid Base64 chars with whitespace
                       " =_-"          // padding and URL‑safe chars
        ).getBytes();

        // Expected: all non‑whitespace bytes are Base64 => true
        assertTrue(Base64.isBase64(mixed), "Array with only Base64 chars (ignoring whitespace) should be recognised as Base64");

        // Introduce an invalid character '!'
        byte[] withInvalid = ("AB!C".getBytes());
        assertFalse(Base64.isBase64(withInvalid), "Array containing an invalid Base64 character should be rejected");
    }
}
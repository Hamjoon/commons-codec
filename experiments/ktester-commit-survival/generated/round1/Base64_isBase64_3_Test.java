package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link Base64#isBase64(byte[])}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>arrays that contain only valid Base64 characters</li>
 *   <li>arrays that contain valid Base64 characters plus whitespace</li>
 *   <li>arrays that contain at least one invalid character</li>
 *   <li>empty arrays</li>
 *   <li>null input (expected NPE)</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class Base64_isBase64_3_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
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
     * Helper that converts a {@code String} to a UTF‑8 encoded byte array.
     */
    private static byte[] toBytes(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void testValidBase64Only() {
        // "TWFu" is "Man" in Base64 and contains only valid characters.
        final byte[] data = toBytes("TWFu");
        assertTrue(Base64.isBase64(data), "All characters are valid Base64");
    }

    @Test
    void testValidBase64WithWhitespace() {
        // Insert spaces, line‑feeds and tabs – all should be ignored.
        final byte[] data = toBytes(" T W F u \n\r\t");
        assertTrue(Base64.isBase64(data), "Whitespace should be ignored");
    }

    @Test
    void testInvalidBase64Character() {
        // '@' is not part of the Base64 alphabet.
        final byte[] data = toBytes("T@Fu");
        assertFalse(Base64.isBase64(data), "Contains an invalid Base64 character");
    }

    @Test
    void testMixedValidAndInvalidCharacters() {
        // Valid characters plus a single invalid one.
        final byte[] data = toBytes("TWFu$");
        assertFalse(Base64.isBase64(data), "Mixed valid and invalid characters should return false");
    }

    @Test
    void testEmptyArray() {
        // An empty array contains no invalid characters, therefore should be true.
        final byte[] data = new byte[0];
        assertTrue(Base64.isBase64(data), "Empty array should be considered Base64");
    }

    @Test
    void testNullArrayThrowsNPE() {
        // The method does not guard against null; a NPE is expected.
        assertThrows(NullPointerException.class, () -> Base64.isBase64(null));
    }
}
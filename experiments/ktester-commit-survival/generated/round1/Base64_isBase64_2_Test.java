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
class Base64_isBase64_2_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
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

    /**
     * A typical valid Base64 string.
     */
    @Test
    void testIsBase64WithValidString() {
        String valid = "VGhpcyBpcyBhIHRlc3Q="; // "This is a test"
        assertTrue(Base64.isBase64(valid), "Valid Base64 string should return true");
    }

    /**
     * A string that contains characters outside the Base64 alphabet.
     */
    @Test
    void testIsBase64WithInvalidString() {
        String invalid = "abc$%def==";
        assertFalse(Base64.isBase64(invalid), "String containing non‑Base64 characters should return false");
    }

    /**
     * Base64 strings may contain whitespace characters; they should be ignored.
     */
    @Test
    void testIsBase64WithWhitespace() {
        String withWhitespace = " V G h p c y B p c y B h I H R l c 3 Q = ";
        assertTrue(Base64.isBase64(withWhitespace), "Base64 string with whitespace should be considered valid");
    }

    /**
     * An empty string does not contain illegal characters, therefore it is considered valid.
     */
    @Test
    void testIsBase64WithEmptyString() {
        String empty = "";
        assertTrue(Base64.isBase64(empty), "Empty string should be treated as valid Base64");
    }

    /**
     * Passing {@code null} should safely return {@code false} rather than throwing an exception.
     */
    @Test
    void testIsBase64WithNull() {
        assertFalse(Base64.isBase64(null), "Null input should return false");
    }

    /**
     * Verify that the byte‑array overload behaves consistently with the String overload.
     */
    @Test
    void testIsBase64ByteArrayConsistency() {
        String base64 = "U29tZSBkYXRhIQ=="; // "Some data!"
        byte[] bytes = StringUtils.getBytesUtf8(base64);
        assertTrue(Base64.isBase64(bytes), "Byte‑array version should return true for valid Base64 data");
        assertEquals(Base64.isBase64(base64), Base64.isBase64(bytes),
                "String and byte‑array overloads should agree");
    }
}
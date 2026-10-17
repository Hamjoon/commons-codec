package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_md5Crypt_5_Test{

    /* --------------------------------------------------------------------- */
    /*  Lifecycle hooks – no special resources are required for these tests */
    /* --------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // Nothing to initialise globally
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
        // Nothing to clean up globally
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Md5Crypt.md5Crypt(byte[], String, String)                     */
    /* --------------------------------------------------------------------- */

    @Test
    void md5CryptWithExplicitPrefixAndSalt() {
        // Arrange
        byte[] password = "secret".getBytes(StandardCharsets.UTF_8);
        String salt = "abcd";                     // 4‑character salt (allowed)
        String prefix = Md5Crypt.MD5_PREFIX;      // "$1$"

        // Act
        String result = Md5Crypt.md5Crypt(password, salt, prefix);

        // Assert – compare with the reference implementation Crypt.crypt
        // Crypt.crypt builds the same string using the same algorithm.
        String expected = Crypt.crypt("secret", "$1$abcd");
        assertEquals(expected, result);
    }

    @Test
    void md5CryptWithNullSaltUsesGeneratedSalt() {
        // Arrange
        byte[] password = "secret".getBytes(StandardCharsets.UTF_8);
        String prefix = Md5Crypt.MD5_PREFIX; // "$1$"

        // Act
        String result = Md5Crypt.md5Crypt(password, null, prefix);

        // Assert – result must match the MD5‑crypt format
        assertNotNull(result);
        assertTrue(result.matches("^\\$1\\$[a-zA-Z0-9./]{0,8}\\$.{1,}$"),
                "Result does not match expected MD5‑crypt pattern: " + result);
    }

    @Test
    void md5CryptWithEmptySaltThrows() {
        // Arrange
        byte[] password = "secret".getBytes(StandardCharsets.UTF_8);
        String emptySalt = "";
        String prefix = Md5Crypt.MD5_PREFIX;

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> Md5Crypt.md5Crypt(password, emptySalt, prefix));
    }

    @Test
    void md5CryptWithNullPasswordThrows() {
        // Arrange
        byte[] nullPassword = null;
        String salt = "foo";
        String prefix = Md5Crypt.MD5_PREFIX;

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> Md5Crypt.md5Crypt(nullPassword, salt, prefix));
    }

    @Test
    void md5CryptZeroesInputArray() {
        // Arrange – fill a buffer with a known pattern
        final byte[] buffer = new byte[64];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) 'A';
        }

        // Act – invoke the method (prefix is MD5_PREFIX, salt is null → generated)
        Md5Crypt.md5Crypt(buffer, null, Md5Crypt.MD5_PREFIX);

        // Assert – the original password array must be cleared to zeros
        byte[] expected = new byte[buffer.length];
        assertArrayEquals(expected, buffer,
                "Password byte array should be zeroed after md5Crypt execution");
    }
}
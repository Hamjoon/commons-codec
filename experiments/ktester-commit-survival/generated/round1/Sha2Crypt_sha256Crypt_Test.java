package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

/**
 * Tests for {@link Sha2Crypt#sha256Crypt(byte[])} and its overloads.
 */
class Sha2Crypt_sha256Crypt_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
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
     * Verify that calling {@code sha256Crypt(byte[])} without an explicit salt
     * returns a non‑null string that starts with the SHA‑256 prefix.
     */
    @Test
    void testSha256Crypt_NoSalt() {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        String result = Sha2Crypt.sha256Crypt(key);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.startsWith(Sha2Crypt.SHA256_PREFIX),
                "Result should start with the SHA‑256 prefix \"$5$\"");
    }

    /**
     * Verify that providing an explicit salt results in a hash that contains
     * the supplied salt and the correct prefix.
     */
    @Test
    void testSha256Crypt_WithExplicitSalt() {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        // Salt without the "$5$" prefix – the implementation adds it internally.
        String salt = "mysalt123";
        String result = Sha2Crypt.sha256Crypt(key, salt);
        assertNotNull(result, "Result must not be null");
        // Expected format: $5$<salt>$<hash>
        String expectedPrefix = Sha2Crypt.SHA256_PREFIX + salt + "$";
        assertTrue(result.startsWith(expectedPrefix),
                "Result should start with the expected prefix: " + expectedPrefix);
    }

    /**
     * Verify that using the same key and the same explicit salt yields the same
     * hash value (deterministic behaviour).
     */
    @Test
    void testSha256Crypt_DeterministicWithSameSalt() {
        byte[] key = "anotherPassword".getBytes(StandardCharsets.UTF_8);
        String salt = "deterministicSalt";

        String first = Sha2Crypt.sha256Crypt(key, salt);
        String second = Sha2Crypt.sha256Crypt(key, salt);

        assertEquals(first, second, "Hashes with identical key and salt must be equal");
    }
}
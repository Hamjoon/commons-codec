package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha512Crypt_3_Test{

    /** A deterministic Random used for reproducible salt generation */
    private Random deterministicRandom;

    /** Regular expression that matches a valid SHA‑512 crypt string.
     *  Example: $6$abcdefgh$<hashed‑value>
     */
    private static final Pattern SHA512_CRYPT_PATTERN =
            Pattern.compile("^\\$6\\$([A-Za-z0-9./]{8})\\$[A-Za-z0-9./]+$");

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Seeded Random guarantees the same “random” salt on every run.
        deterministicRandom = new Random(0L);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void sha512Crypt_WithExplicitSalt_ReturnsCorrectlyPrefixedHash() {
        // GIVEN
        byte[] key = "secretPassword".getBytes();
        // Salt must already contain the "$6$" prefix and a trailing '$' as required by the algorithm.
        String salt = "$6$mySalt12$";

        // WHEN
        String result = Sha2Crypt.sha512Crypt(key, salt, deterministicRandom);

        // THEN
        assertNotNull(result, "Result must not be null");
        assertTrue(result.startsWith(salt),
                () -> "Result should start with the supplied salt. Expected prefix: " + salt + " but got: " + result);
        // The rest of the string after the salt should be the hashed part – we only check that something follows.
        assertTrue(result.length() > salt.length(),
                () -> "Result should contain a hash after the salt. Result: " + result);
    }

    @Test
    void sha512Crypt_WithNullSalt_GeneratesRandomSaltAndValidFormat() {
        // GIVEN
        byte[] key = "anotherSecret".getBytes();
        String salt = null; // trigger random‑salt generation

        // WHEN
        String result = Sha2Crypt.sha512Crypt(key, salt, deterministicRandom);

        // THEN
        assertNotNull(result, "Result must not be null");
        // Verify the overall format matches the expected pattern.
        assertTrue(SHA512_CRYPT_PATTERN.matcher(result).matches(),
                () -> "Result does not match SHA‑512 crypt pattern. Result: " + result);
        // Extract the generated salt and ensure it is exactly 8 characters long (as per B64.getRandomSalt(8,…)).
        String generatedSalt = result.substring(3, 11); // "$6$".length() == 3
        assertEquals(8, generatedSalt.length(),
                () -> "Generated salt should be 8 characters long. Found: '" + generatedSalt + "'");
    }

    @Test
    void sha512Crypt_WithEmptyKey_ProducesNonNullHash() {
        // GIVEN
        byte[] emptyKey = new byte[0];
        String salt = "$6$emptyKey$";

        // WHEN
        String result = Sha2Crypt.sha512Crypt(emptyKey, salt, deterministicRandom);

        // THEN
        assertNotNull(result, "Result must not be null for empty key");
        assertTrue(result.startsWith(salt),
                () -> "Result should start with the supplied salt even when key is empty.");
    }
}
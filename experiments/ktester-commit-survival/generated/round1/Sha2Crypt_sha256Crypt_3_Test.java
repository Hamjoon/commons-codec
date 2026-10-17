package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha256Crypt_3_Test{

    private static final byte[] KEY = "password".getBytes(StandardCharsets.UTF_8);
    private Random deterministicRandom;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a deterministic Random so that generated salts are reproducible
        deterministicRandom = new Random(0L);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    /**
     * Verify that when a non‑null salt is supplied, the result contains that salt
     * and uses the SHA‑256 prefix "$5$".
     */
    @Test
    void testSha256Crypt_WithExplicitSalt() {
        String salt = Sha2Crypt.SHA256_PREFIX + "mysalt";
        String result = Sha2Crypt.sha256Crypt(KEY, salt, new SecureRandom());

        assertNotNull(result, "Result should not be null");
        // Expected format: $5$mysalt$<hash>
        assertTrue(result.startsWith(salt + "$"),
                () -> "Result should start with the provided salt followed by '$': " + result);
        assertTrue(result.contains(Sha2Crypt.SHA256_PREFIX),
                () -> "Result should contain the SHA‑256 prefix \"$5$\": " + result);
    }

    /**
     * Verify that when the salt argument is {@code null}, a random 8‑character
     * salt is generated (using the supplied {@link Random}) and the result
     * starts with the SHA‑256 prefix.
     */
    @Test
    void testSha256Crypt_WithNullSaltGeneratesRandomSalt() {
        String result = Sha2Crypt.sha256Crypt(KEY, null, deterministicRandom);

        assertNotNull(result, "Result should not be null");
        // Result must start with "$5$"
        assertTrue(result.startsWith(Sha2Crypt.SHA256_PREFIX),
                () -> "Result should start with the SHA‑256 prefix \"$5$\": " + result);

        // Extract the generated salt (the part between the first and second '$')
        // Format: $5$<salt>$<hash>
        String[] parts = result.split("\\$");
        // parts[0] is empty because string starts with '$'
        // parts[1] -> "5"
        // parts[2] -> generated salt
        assertEquals(4, parts.length,
                () -> "Result should have four '$' separated parts: " + result);
        String generatedSalt = parts[2];
        assertEquals(8, generatedSalt.length(),
                () -> "Generated salt should be 8 characters long: " + generatedSalt);
    }
}
package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_apr1Crypt_2_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
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
     * Verify that the simple {@code apr1Crypt(String)} overload produces a result
     * that follows the expected APR‑1 format.
     */
    @Test
    void testApr1CryptStringFormat() {
        String result = Md5Crypt.apr1Crypt("secret");
        assertNotNull(result);
        assertTrue(result.matches("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.+$"),
                "Result does not match APR1 format: " + result);
    }

    /**
     * When a salt is supplied the returned hash must start with that salt.
     */
    @Test
    void testApr1CryptWithExplicitSalt() {
        String salt = "abcd1234";
        String result1 = Md5Crypt.apr1Crypt("password".getBytes(StandardCharsets.UTF_8), salt);
        String result2 = Md5Crypt.apr1Crypt("password".getBytes(StandardCharsets.UTF_8), salt);
        assertEquals("$apr1$" + salt + "$", result1.substring(0, ("$apr1$" + salt + "$").length()),
                "Hash does not contain the expected salt prefix");
        // The algorithm is deterministic for a given salt, so both calls must be equal
        assertEquals(result1, result2, "Two calls with the same salt produced different results");
    }

    /**
     * The {@code apr1Crypt(String)} overload should reject {@code null} input.
     */
    @Test
    void testApr1CryptNullInput() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.apr1Crypt((String) null));
    }

    /**
     * An empty salt is illegal – the method must throw {@link IllegalArgumentException}.
     */
    @Test
    void testApr1CryptEmptySalt() {
        assertThrows(IllegalArgumentException.class,
                () -> Md5Crypt.apr1Crypt("any".getBytes(StandardCharsets.UTF_8), ""));
    }

    /**
     * Using a deterministic {@link Random} instance should give reproducible results.
     * This test exercises the overload that accepts a {@code Random} and checks that
     * two invocations with the same seed produce identical hashes.
     */
    @Test
    void testApr1CryptWithFixedRandom() {
        Random deterministicRandom = new Random(12345L);
        byte[] password = "fixedRandom".getBytes(StandardCharsets.UTF_8);

        String hash1 = Md5Crypt.apr1Crypt(password, deterministicRandom);
        // Reset the Random to the same seed for a second call
        deterministicRandom = new Random(12345L);
        String hash2 = Md5Crypt.apr1Crypt(password, deterministicRandom);

        assertEquals(hash1, hash2, "Hashes differ when using the same deterministic Random instance");
        assertTrue(hash1.matches("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.+$"),
                "Result does not match APR1 format: " + hash1);
    }
}
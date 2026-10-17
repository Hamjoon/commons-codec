package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_md5Crypt_3_Test{

    /** A deterministic Random used for reproducible salt generation. */
    private static Random deterministicRandom;

    /** Regular expression that matches the full MD5‑crypt output. */
    private static final Pattern MD5_CRYPT_PATTERN = Pattern.compile("^\\$1\\$[A-Za-z0-9./]{8}\\$.{1,}$");

    @BeforeAll
    static void setupBeforeAll() {
        // Seed is fixed so that the generated salt is predictable across runs.
        deterministicRandom = new Random(0L);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise per‑test at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verifies that {@code md5Crypt(byte[], Random)} returns a string that
     * <ul>
     *   <li>starts with the MD5‑crypt prefix {@code "$1$"}</li>
     *   <li>contains a salt of exactly eight characters</li>
     *   <li>matches the overall MD5‑crypt format</li>
     * </ul>
     */
    @Test
    void testMd5CryptGeneratesSaltOfCorrectLength() {
        byte[] password = "secret".getBytes();
        String result = Md5Crypt.md5Crypt(password, deterministicRandom);

        // Verify overall format.
        assertTrue(MD5_CRYPT_PATTERN.matcher(result).matches(),
                "Result does not match MD5‑crypt pattern: " + result);

        // Extract the salt part (the part between the first and second '$')
        String[] parts = result.split("\\$");
        // parts[0] is empty (string starts with $), parts[1] is "1", parts[2] is the salt
        assertEquals(4, parts.length, "Unexpected number of '$' separators in result");
        String salt = parts[2];
        assertEquals(8, salt.length(), "Salt length should be 8 characters");
    }

    /**
     * The MD5‑crypt implementation zeroes out the password byte array after
     * processing. This test ensures that behaviour.
     */
    @Test
    void testMd5CryptZeroesInput() {
        byte[] password = new byte[64];
        // Fill with a non‑zero value to make the check meaningful.
        for (int i = 0; i < password.length; i++) {
            password[i] = (byte) 0x5A;
        }

        // Keep a copy for later comparison (should become all zeros).
        byte[] originalReference = password.clone();

        // Call the method under test.
        Md5Crypt.md5Crypt(password, deterministicRandom);

        // After the call the original array must be zero‑filled.
        assertArrayEquals(new byte[originalReference.length], password,
                "Password array was not cleared after md5Crypt");
    }

    /**
     * Passing a {@code null} password array must result in a {@link NullPointerException}.
     */
    @Test
    void testMd5CryptNullKeyThrows() {
        assertThrows(NullPointerException.class,
                () -> Md5Crypt.md5Crypt((byte[]) null, deterministicRandom));
    }

    /**
     * Passing a {@code null} {@link Random} instance must result in a {@link NullPointerException}.
     */
    @Test
    void testMd5CryptNullRandomThrows() {
        byte[] password = "secret".getBytes();
        assertThrows(NullPointerException.class,
                () -> Md5Crypt.md5Crypt(password, (Random) null));
    }

    /**
     * An empty password should still produce a valid MD5‑crypt string.
     */
    @Test
    void testMd5CryptEmptyPassword() {
        byte[] empty = new byte[0];
        String result = Md5Crypt.md5Crypt(empty, deterministicRandom);
        assertTrue(MD5_CRYPT_PATTERN.matcher(result).matches(),
                "Empty password did not produce a valid MD5‑crypt string");
    }
}
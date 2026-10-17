package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_md5Crypt_2_Test{

    /** Pattern that matches a valid MD5‑crypt string produced by {@code Md5Crypt.md5Crypt(byte[])}. */
    private static final Pattern MD5_CRYPT_PATTERN = Pattern.compile("^\\$1\\$[A-Za-z0-9./]{0,8}\\$.+$");

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for the static Md5Crypt methods.
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

    @Test
    void testMd5CryptNullByteArrayThrowsNPE() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.md5Crypt((byte[]) null));
    }

    @Test
    void testMd5CryptReturnsValidFormat() {
        // Use a deterministic input; the salt is random, so we only verify the format.
        byte[] secret = "secret".getBytes(StandardCharsets.UTF_8);
        String result = Md5Crypt.md5Crypt(secret);
        assertNotNull(result, "Result should not be null");
        assertTrue(MD5_CRYPT_PATTERN.matcher(result).matches(),
                "Result should match MD5‑crypt pattern, but was: " + result);
    }

    @Test
    void testMd5CryptZeroesOutInputBuffer() {
        // Prepare a buffer filled with a known pattern.
        byte[] buffer = new byte[128];
        Arrays.fill(buffer, (byte) 'A');

        // Call the method – it should zero‑out the buffer on return.
        String hash = Md5Crypt.md5Crypt(buffer);
        assertNotNull(hash, "Hash must not be null");

        // Verify that the original buffer has been cleared.
        byte[] expectedZeroes = new byte[buffer.length];
        assertArrayEquals(expectedZeroes, buffer,
                "Input buffer should be zeroed out after md5Crypt execution");
    }

    @Test
    void testMd5CryptConsistentWithExplicitSalt() {
        // When a salt is supplied, the output must be reproducible.
        byte[] data = "password".getBytes(StandardCharsets.UTF_8);
        String salt = Md5Crypt.MD5_PREFIX + "mysalt"; // "$1$mysalt"
        String first = Md5Crypt.md5Crypt(data, salt);
        String second = Md5Crypt.md5Crypt(data, salt);
        assertEquals(first, second, "With an explicit salt the hash must be deterministic");
        assertTrue(first.startsWith("$1$mysalt$"),
                "Hash should start with the provided salt prefix");
    }
}
package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_md5Crypt_Test {

    /**
     * Supplies the different invalid‑salt cases.
     *
     * <ul>
     *   <li>Salt without characters after the prefix.</li>
     *   <li>Salt longer than 8 characters after the prefix.</li>
     *   <li>Salt using a different prefix than the supplied one.</li>
     * </ul>
     */
    private static Stream<Arguments> invalidSaltProvider() {
        byte[] dummyKey = "x".getBytes(StandardCharsets.UTF_8);
        return Stream.of(// 1) Prefix present but no following characters (length 0)
        Arguments.of(dummyKey, "$1$", "Salt contains prefix but no characters (length 0)"), // 2) More than 8 characters after the prefix
        Arguments.of(dummyKey, "$1$123456789", "Salt has >8 characters after prefix"), // 3) Different prefix than the supplied one
        Arguments.of(dummyKey, "$apr1$valid", "Salt uses a different prefix than the supplied prefix"));
    }

    @Test
    void md5Crypt_WithExplicitSaltAndPrefix_ProducesExpectedStructure() {
        // GIVEN
        final byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        // prefix + 4‑character salt
        final String salt = "$1$abcd";
        // "$1$"
        final String prefix = Md5Crypt.MD5_PREFIX;
        // not used because salt is non‑null
        final Random random = new SecureRandom();
        // WHEN
        final String result = Md5Crypt.md5Crypt(key, salt, prefix, random);
        // THEN
        // Result must start with the supplied prefix and salt (without the leading "$1$" part of the salt string)
        assertTrue(result.startsWith("$1$abcd$"), "Result should start with the supplied prefix and salt");
        // The rest of the hash must be a Base64‑like string of length 22 (MD5‑crypt output)
        final String hashPart = result.substring("$1$abcd$".length());
        assertEquals(22, hashPart.length(), "Hash part length should be 22 characters");
        assertTrue(Pattern.matches("[./A-Za-z0-9]{22}", hashPart), "Hash part should contain only the allowed characters");
    }

    @Test
    void md5Crypt_WithNullSalt_GeneratesRandomSaltAndMatchesPattern() {
        // GIVEN
        final byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        // trigger random salt generation
        final String salt = null;
        // "$1$"
        final String prefix = Md5Crypt.MD5_PREFIX;
        // true randomness – we only check format
        final Random random = new SecureRandom();
        // WHEN
        final String result = Md5Crypt.md5Crypt(key, salt, prefix, random);
        // THEN
        // The result must match the generic MD5‑crypt format: $1$<1‑8 chars>$<22 chars>
        final String regex = "^\\$1\\$[./A-Za-z0-9]{1,8}\\$[./A-Za-z0-9]{22}$";
        assertTrue(Pattern.matches(regex, result), "Result should follow the MD5‑crypt format with a random salt");
    }

    @Test
    void md5Crypt_WithInvalidSalt_ThrowsIllegalArgumentException() {
        // GIVEN
        final byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        // Salt does not start with the required prefix "$1$"
        final String badSalt = "badSaltValue";
        final String prefix = Md5Crypt.MD5_PREFIX;
        final Random random = new SecureRandom();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(key, badSalt, prefix, random), "An illegal salt value should cause IllegalArgumentException");
    }

    @Test
    void md5Crypt_ZeroesOutPasswordArrayAfterProcessing() {
        // GIVEN
        final byte[] key = new byte[64];
        // Fill with a known pattern
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }
        final String salt = "$1$xyz";
        final String prefix = Md5Crypt.MD5_PREFIX;
        final Random random = new SecureRandom();
        // WHEN
        Md5Crypt.md5Crypt(key, salt, prefix, random);
        // THEN
        // After the call the original array must be cleared (all zeros)
        for (byte b : key) {
            assertEquals(0, b, "Password byte array must be cleared after md5Crypt");
        }
    }

    @Test
    void md5Crypt_NullKeyBytes_ThrowsNullPointerException() {
        // GIVEN
        final byte[] key = null;
        final String salt = "$1$someSalt";
        final String prefix = Md5Crypt.MD5_PREFIX;
        final Random random = new Random();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> Md5Crypt.md5Crypt(key, salt, prefix, random), "Passing a null keyBytes array must result in NullPointerException");
    }

    @Test
    void md5Crypt_NullPrefix_ThrowsNullPointerException() {
        // GIVEN
        final byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        final String salt = "$1$valid";
        final String prefix = null;
        final Random random = new Random();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> Md5Crypt.md5Crypt(key, salt, prefix, random), "Passing a null prefix must result in NullPointerException");
    }

    @Test
    void md5Crypt_NullRandomWhenSaltIsNull_ThrowsNullPointerException() {
        // GIVEN
        final byte[] key = "a".getBytes(StandardCharsets.UTF_8);
        // triggers random‑salt generation
        final String salt = null;
        final String prefix = Md5Crypt.MD5_PREFIX;
        // illegal – B64.getRandomSalt will dereference it
        final Random random = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> Md5Crypt.md5Crypt(key, salt, prefix, random), "When salt is null, a null Random must cause NullPointerException");
    }

    /**
     * Parameterized test covering the various illegal‑salt scenarios described in the JSON.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("invalidSaltProvider")
    void md5Crypt_InvalidSaltScenarios_ThrowsIllegalArgumentException(byte[] keyBytes, String salt, String description) {
        final String prefix = Md5Crypt.MD5_PREFIX;
        final Random random = new Random();
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(keyBytes, salt, prefix, random), description);
    }
}

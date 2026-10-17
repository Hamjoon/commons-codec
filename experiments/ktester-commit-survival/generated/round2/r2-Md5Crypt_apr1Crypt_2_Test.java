package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_apr1Crypt_2_Test {

    /**
     * Regular expression that matches the APR‑1 hash format.
     */
    private static final Pattern APR1_PATTERN = Pattern.compile("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.+$");

    /**
     * Extracts the salt part (the characters between the first two '$').
     */
    private static final Pattern SALT_EXTRACTOR = Pattern.compile("^\\$apr1\\$([^$]+)\\$.*$");

    /**
     * Allowed characters for the salt (exactly 8 chars).
     */
    private static final Pattern SALT_CHARSET_PATTERN = Pattern.compile("^[./0-9A-Za-z]{8}$");

    /**
     * Allowed characters for the hash part (exactly 22 chars).
     */
    private static final Pattern HASH_CHARSET_PATTERN = Pattern.compile("^[./0-9A-Za-z]{22}$");

    /**
     * Helper that extracts the salt part from an APR‑1 hash.
     *
     * @param hash the full hash string
     * @return the salt (the characters between the first two '$')
     */
    private static String extractSalt(String hash) {
        Matcher m = SALT_EXTRACTOR.matcher(hash);
        assertTrue(m.matches(), "Hash does not match expected APR‑1 format: " + hash);
        return m.group(1);
    }

    /**
     * Very long passwords – 255 and 5 000 characters.
     */
    static Stream<String> longPasswordProvider() {
        return Stream.of("a".repeat(255), "a".repeat(5000));
    }

    @Test
    void apr1CryptStringProducesValidHash() {
        String password = "secretPassword";
        String hash = Md5Crypt.apr1Crypt(password);
        assertNotNull(hash, "Hash must not be null");
        assertTrue(APR1_PATTERN.matcher(hash).matches(), "Hash should match APR‑1 pattern, but was: " + hash);
    }

    @Test
    void apr1CryptEmptyStringProducesValidHash() {
        String hash = Md5Crypt.apr1Crypt("");
        assertNotNull(hash);
        assertTrue(APR1_PATTERN.matcher(hash).matches(), "Empty password should still produce a valid APR‑1 hash");
    }

    @Test
    void apr1CryptNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.apr1Crypt(null), "Calling apr1Crypt with null must throw NullPointerException");
    }

    @Test
    void apr1CryptGeneratesDifferentSaltsForSamePassword() {
        String password = "samePassword";
        String firstHash = Md5Crypt.apr1Crypt(password);
        String secondHash = Md5Crypt.apr1Crypt(password);
        // Extract the salts from the two hashes.
        String firstSalt = extractSalt(firstHash);
        String secondSalt = extractSalt(secondHash);
        // In the overwhelming majority of cases the salts will differ because a random
        // generator is used. If they happen to be equal, the test is retried once to
        // avoid flaky failures.
        if (firstSalt.equals(secondSalt)) {
            secondHash = Md5Crypt.apr1Crypt(password);
            secondSalt = extractSalt(secondHash);
        }
        assertNotEquals(firstSalt, secondSalt, () -> String.format("Two hashes for the same password should have different salts, but both were \"%s\".", firstSalt));
    }

    /**
     * Typical passwords including ASCII, symbols and dollar signs.
     */
    @ParameterizedTest(name = "APR‑1 hash for typical password \"{0}\"")
    @ValueSource(strings = { "password123", "p@$$w0rd!#%&*", "pa$$w0rd$with$dollar" })
    void apr1CryptTypicalPasswords(String password) {
        String hash = Md5Crypt.apr1Crypt(password);
        assertNotNull(hash, "Hash must not be null for password: " + password);
        assertTrue(APR1_PATTERN.matcher(hash).matches(), "Hash should match APR‑1 pattern for password \"" + password + "\", but was: " + hash);
    }

    /**
     * Unicode passwords (non‑ASCII characters and emojis).
     */
    @ParameterizedTest(name = "APR‑1 hash for Unicode password \"{0}\"")
    @ValueSource(strings = { "pässwördß", "😀🚀🌟" })
    void apr1CryptUnicodePasswords(String password) {
        String hash = Md5Crypt.apr1Crypt(password);
        assertNotNull(hash, "Hash must not be null for Unicode password: " + password);
        assertTrue(APR1_PATTERN.matcher(hash).matches(), "Hash should match APR‑1 pattern for Unicode password \"" + password + "\", but was: " + hash);
    }

    @ParameterizedTest(name = "APR‑1 hash for long password (length {0})")
    @MethodSource("longPasswordProvider")
    void apr1CryptLongPasswords(String password) {
        String hash = Md5Crypt.apr1Crypt(password);
        assertNotNull(hash, "Hash must not be null for long password");
        assertTrue(APR1_PATTERN.matcher(hash).matches(), "Hash should match APR‑1 pattern for long password (length " + password.length() + "), but was: " + hash);
    }

    /**
     * Verifies the exact structure: four parts when split on '$'.
     */
    @Test
    void apr1CryptResultStructure() {
        String hash = Md5Crypt.apr1Crypt("anyPassword");
        String[] parts = hash.split("\\$");
        // Expected: ["", "apr1", "<8‑char‑salt>", "<22‑char‑hash>"]
        assertEquals(4, parts.length, "APR‑1 hash should contain exactly three '$' characters (four parts after split)");
        assertEquals("apr1", parts[1], "Second part must be the APR‑1 prefix");
        assertEquals(8, parts[2].length(), "Salt part must be exactly 8 characters");
        assertEquals(22, parts[3].length(), "Hash part must be exactly 22 characters");
    }

    /**
     * Ensures the generated salt uses only the allowed character set.
     */
    @Test
    void apr1CryptSaltCharacterSet() {
        String hash = Md5Crypt.apr1Crypt("anyPassword");
        String salt = extractSalt(hash);
        assertTrue(SALT_CHARSET_PATTERN.matcher(salt).matches(), "Salt must consist of exactly 8 characters from [./0-9A-Za-z]; was: \"" + salt + "\"");
    }

    /**
     * Ensures the hash component uses only the allowed character set.
     */
    @Test
    void apr1CryptHashCharacterSetAndLength() {
        String hash = Md5Crypt.apr1Crypt("anyPassword");
        String[] parts = hash.split("\\$");
        String hashPart = parts[3];
        assertTrue(HASH_CHARSET_PATTERN.matcher(hashPart).matches(), "Hash part must consist of exactly 22 characters from [./0-9A-Za-z]; was: \"" + hashPart + "\"");
    }
}

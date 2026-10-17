package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha512Crypt_3_Test {

    /**
     * Regular expression for a valid SHA‑512‑crypt string.
     *  $6$ + salt (8‑64 chars allowed) + $ + 86‑char hash.
     */
    private static final Pattern VALID_SHA512_CRYPT = Pattern.compile("^\\$6\\$[^$]{1,16}\\$[./A-Za-z0-9]{86}$");

    /**
     * Returns the hash part (the string after the last '$') of a crypt output.
     */
    private static String hashPart(String crypt) {
        return crypt.substring(crypt.lastIndexOf('$') + 1);
    }

    /**
     * Supplies two variants: salt already containing the "$6$" prefix and salt without it.
     */
    private static Stream<Arguments> provideSaltWithAndWithoutPrefix() {
        return Stream.of(// salt already contains the prefix, expected result starts with it directly
        Arguments.of("$6$mysalt", "$6$mysalt$"), // salt without prefix, sha2Crypt is expected to add the prefix automatically
        Arguments.of("mysalt", "$6$mysalt$"));
    }

    @Test
    void testSha512CryptWithProvidedSalt() {
        // GIVEN
        byte[] keyBytes = "secretPassword".getBytes(StandardCharsets.UTF_8);
        String salt = "$6$myCustomSalt$";
        // random is not used because salt is supplied
        Random random = new SecureRandom();
        // WHEN
        String result = Sha2Crypt.sha512Crypt(keyBytes, salt, random);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertTrue(result.startsWith(salt), () -> "Result should start with the provided salt. Expected prefix: " + salt + " but was: " + result);
        // The result must also contain the SHA‑512 prefix "$6$"
        assertTrue(result.contains(Sha2Crypt.SHA512_PREFIX), () -> "Result should contain the SHA‑512 prefix \"" + Sha2Crypt.SHA512_PREFIX + "\"");
    }

    @Test
    void testSha512CryptWithNullSaltGeneratesPrefix() {
        // GIVEN
        byte[] keyBytes = "anotherSecret".getBytes(StandardCharsets.UTF_8);
        String salt = null;
        // used only if salt is null
        Random random = new SecureRandom();
        // WHEN
        String result = Sha2Crypt.sha512Crypt(keyBytes, salt, random);
        // THEN
        assertNotNull(result, "Result should not be null");
        // When salt is null the method must prepend the SHA‑512 prefix "$6$"
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX), () -> "Result should start with the SHA‑512 prefix when salt is null. Expected prefix: " + Sha2Crypt.SHA512_PREFIX + " but was: " + result);
    }

    /**
     * ----------- 1. Salt is null – random salt generation ------------
     */
    @Test
    void testNullSaltGeneratesRandomAndValidFormat() {
        // GIVEN
        byte[] keyBytes = "password".getBytes(StandardCharsets.UTF_8);
        String salt = null;
        // non‑null random
        Random random = new SecureRandom();
        // WHEN
        String result = Sha2Crypt.sha512Crypt(keyBytes, salt, random);
        // THEN – result must start with the SHA‑512 prefix
        assertNotNull(result);
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX), () -> "Result should start with the SHA‑512 prefix \"$6$\"");
        // AND – salt part must be exactly 8 base‑64 characters (B64.getRandomSalt(8,…))
        //    pattern: $6$<8‑char-salt>$<hash>
        assertTrue(Pattern.compile("^\\$6\\$[./A-Za-z0-9]{8}\\$.*$").matcher(result).matches(), () -> "Result should contain an 8‑character random salt: " + result);
        // AND – hash part length must be 86 characters (standard SHA‑512‑crypt output)
        String hash = hashPart(result);
        assertEquals(86, hash.length(), () -> "Hash part length should be 86 but was " + hash.length());
    }

    @Test
    void testNullSaltAndNullRandomThrowsNPE() {
        // GIVEN
        byte[] keyBytes = "password".getBytes(StandardCharsets.UTF_8);
        String salt = null;
        // both null
        Random random = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> Sha2Crypt.sha512Crypt(keyBytes, salt, random), "Calling sha512Crypt with null salt and null Random must throw NPE");
    }

    @ParameterizedTest
    @MethodSource("provideSaltWithAndWithoutPrefix")
    void testProvidedSaltIsHonoured(String providedSalt, String expectedPrefix) {
        // GIVEN
        byte[] keyBytes = "password".getBytes(StandardCharsets.UTF_8);
        // random is ignored because salt is supplied
        Random random = new SecureRandom();
        // WHEN
        String result = Sha2Crypt.sha512Crypt(keyBytes, providedSalt, random);
        // THEN – result must start with the expected prefix (always "$6$")
        assertNotNull(result);
        assertTrue(result.startsWith(expectedPrefix), () -> "Result should start with expected prefix '" + expectedPrefix + "' but was: " + result);
        // AND – the salt that appears after the prefix must be exactly the literal salt string
        // (without any extra "$" characters inside)
        String[] parts = result.split("\\$");
        // parts[0] is empty (because string starts with $), parts[1] = "6", parts[2] = salt, parts[3] = hash
        assertEquals(4, parts.length, () -> "Result should contain exactly three '$' separators: " + result);
        assertEquals(expectedPrefix.substring(3, expectedPrefix.length() - 1), parts[2], () -> "Embedded salt should match the supplied one");
        // AND – hash part length must be 86 characters
        assertEquals(86, parts[3].length(), () -> "Hash part must be 86 characters long");
    }

    /**
     * ----------- 3. Key bytes are null ------------------------------
     */
    @Test
    void testNullKeyBytesThrowsIllegalArgumentException() {
        // GIVEN
        byte[] keyBytes = null;
        String salt = "$6$test";
        Random random = new SecureRandom();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(keyBytes, salt, random), "Passing a null keyBytes must result in IllegalArgumentException");
    }

    /**
     * ----------- 4. Random is null but salt provided -----------------
     */
    @Test
    void testNullRandomIsIgnoredWhenSaltProvided() {
        // GIVEN
        byte[] keyBytes = "pass".getBytes(StandardCharsets.UTF_8);
        String salt = "$6$explicit";
        // should be safe because salt is supplied
        Random random = null;
        // WHEN
        String result = Sha2Crypt.sha512Crypt(keyBytes, salt, random);
        // THEN – result must start with the supplied salt and be a valid hash
        assertNotNull(result);
        assertTrue(result.startsWith("$6$explicit$"), () -> "Result should start with the supplied salt '$6$explicit$' but was: " + result);
        assertEquals(86, hashPart(result).length(), "Hash part must be 86 characters long");
    }

    /**
     * ----------- 5. Salt without required prefix --------------------
     */
    @Test
    void testSaltWithoutRequiredPrefixThrowsIllegalArgumentException() {
        // GIVEN
        byte[] keyBytes = "pass".getBytes(StandardCharsets.UTF_8);
        // missing "$6$" prefix
        String salt = "mysalt";
        Random random = new SecureRandom();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(keyBytes, salt, random), "Salt not starting with \"$6$\" must cause IllegalArgumentException");
    }

    /**
     * ----------- 6. Salt exceeding maximum length --------------------
     */
    @Test
    void testTooLongSaltThrowsIllegalArgumentException() {
        // GIVEN
        byte[] keyBytes = "pass".getBytes(StandardCharsets.UTF_8);
        // 17 characters after prefix (limit is 16)
        String salt = "$6$12345678901234567";
        Random random = new SecureRandom();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(keyBytes, salt, random), "Salt longer than 16 characters must cause IllegalArgumentException");
    }

    /**
     * ----------- 7. Salt containing illegal characters ----------------
     */
    @Test
    void testSaltWithInternalDollarThrowsIllegalArgumentException() {
        // GIVEN
        byte[] keyBytes = "pass".getBytes(StandardCharsets.UTF_8);
        // internal '$' not allowed (it would terminate the salt prematurely)
        String salt = "$6$invalid$salt";
        Random random = new SecureRandom();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(keyBytes, salt, random), "Salt containing an internal '$' must cause IllegalArgumentException");
    }

    /**
     * ----------- 8. Salt with malformed rounds specification ----------
     */
    @Test
    void testSaltWithNonNumericRoundsThrowsIllegalArgumentException() {
        // GIVEN
        byte[] keyBytes = "pass".getBytes(StandardCharsets.UTF_8);
        // malformed rounds part: rounds=abc (non‑numeric)
        String salt = "$6$rounds=abc$mysalt";
        Random random = new SecureRandom();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(keyBytes, salt, random), "Malformed rounds specification must cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Additional sanity check – the public overload without Random
     * ------------------------------------------------------------- */
    @Test
    void testSha512CryptWithoutRandomUsesSecureRandomInternally() {
        // GIVEN
        byte[] keyBytes = "secure".getBytes(StandardCharsets.UTF_8);
        // trigger random generation
        String salt = null;
        // WHEN – use the two‑argument overload which internally creates a SecureRandom
        String result = Sha2Crypt.sha512Crypt(keyBytes, salt);
        // THEN – same format checks as for the three‑argument version
        assertNotNull(result);
        assertTrue(VALID_SHA512_CRYPT.matcher(result).matches(), () -> "Result should match the full SHA‑512‑crypt pattern, but was: " + result);
    }
}

package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha512Crypt_2_Test {

    static Stream<Arguments> nullSaltProvider() {
        return Stream.of(Arguments.of("secret".getBytes(StandardCharsets.UTF_8)), // empty password
        Arguments.of(new byte[0]));
    }

    /**
     * Verify that calling {@code sha512Crypt} with a {@code null} salt generates a
     * result that starts with the SHA‑512 prefix ($6$) and follows the expected
     * crypt format (three '$' separators).
     */
    @Test
    void testSha512CryptWithNullSalt() {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        // salt == null
        String result = Sha2Crypt.sha512Crypt(key);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX), "Result should start with the SHA‑512 prefix \"$6$\"");
        // The crypt string format is $6$salt$hash → exactly three '$' characters
        long dollarCount = result.chars().filter(ch -> ch == '$').count();
        assertEquals(3, dollarCount, "Result should contain exactly three '$' separators");
    }

    /**
     * Verify that providing an explicit salt yields a result that embeds that
     * salt (including the prefix) at the beginning of the returned string.
     */
    @Test
    void testSha512CryptWithGivenSalt() {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        // e.g. "$6$testsalt"
        String explicitSalt = Sha2Crypt.SHA512_PREFIX + "testsalt";
        String result = Sha2Crypt.sha512Crypt(key, explicitSalt);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.startsWith(explicitSalt), "Result should start with the supplied salt");
        // Ensure the format still contains exactly three '$' separators
        long dollarCount = result.chars().filter(ch -> ch == '$').count();
        assertEquals(3, dollarCount, "Result should contain exactly three '$' separators");
    }

    /**
     * When the supplied salt is {@code null} the method must generate a random
     * 8‑character base‑64 salt and prepend the SHA‑512 prefix.
     */
    @ParameterizedTest(name = "{index}: keyBytes={0}")
    @MethodSource("nullSaltProvider")
    void testNullSaltGeneratesRandomSalt(byte[] keyBytes) {
        String result = Sha2Crypt.sha512Crypt(keyBytes, null);
        assertNotNull(result);
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX), "Result must start with SHA‑512 prefix");
        // Expected pattern: $6$<8 base‑64 chars>$
        assertTrue(result.matches("^\\$6\\$[A-Za-z0-9./]{8}\\$.*$"), "Result should contain a random 8‑character salt after the prefix");
    }

    @Test
    void testProvidedSaltWithPrefix() {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        String salt = Sha2Crypt.SHA512_PREFIX + "mysalt";
        String result = Sha2Crypt.sha512Crypt(key, salt);
        assertTrue(result.startsWith(salt + "$"), "Result must start with the exact supplied salt followed by '$'");
    }

    @Test
    void testProvidedSaltWithoutPrefix() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        // missing "$6$"
        String salt = "mysalt";
        String result = Sha2Crypt.sha512Crypt(key, salt);
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX + "mysalt$"), "Result must prepend the SHA‑512 prefix when missing");
    }

    @Test
    void testProvidedSaltTrailingDollar() {
        byte[] key = "test".getBytes(StandardCharsets.UTF_8);
        String salt = Sha2Crypt.SHA512_PREFIX + "mysalt$";
        String result = Sha2Crypt.sha512Crypt(key, salt);
        // The implementation should treat the empty part after the trailing '$' as an empty salt.
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX + "mysalt$$"), "Result should contain a double '$' when salt already ends with '$'");
    }

    @Test
    void testEmptyStringSalt() {
        byte[] key = "edge".getBytes(StandardCharsets.UTF_8);
        String result = Sha2Crypt.sha512Crypt(key, "");
        // Expected prefix "$6$$"
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX + "$"), "Result should start with \"$6$$\" when an empty salt is supplied");
    }

    @Test
    void testOnlyPrefixSalt() {
        byte[] key = "test".getBytes(StandardCharsets.UTF_8);
        String result = Sha2Crypt.sha512Crypt(key, Sha2Crypt.SHA512_PREFIX);
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX + "$"), "Result should start with \"$6$$\" when salt is only the prefix");
    }

    @Test
    void testRoundsSpecificationValid() {
        byte[] key = "pwd".getBytes(StandardCharsets.UTF_8);
        String salt = Sha2Crypt.SHA512_PREFIX + "rounds=5000$mysalt";
        String result = Sha2Crypt.sha512Crypt(key, salt);
        assertTrue(result.startsWith("$6$rounds=5000$mysalt$"), "Result must preserve the rounds specification in the prefix");
        // Full length for SHA‑512 crypt strings is 108 characters (prefix+hash)
        assertEquals(108, result.length(), "Full SHA‑512 crypt string length should be 108 characters");
    }

    @Test
    void testRoundsSpecificationEmptySalt() {
        byte[] key = "pwd".getBytes(StandardCharsets.UTF_8);
        String salt = Sha2Crypt.SHA512_PREFIX + "rounds=10000$";
        String result = Sha2Crypt.sha512Crypt(key, salt);
        assertTrue(result.startsWith("$6$rounds=10000$$"), "Result should contain an empty salt after a valid rounds specifier");
        assertEquals(108, result.length(), "Result length must still be the full SHA‑512 crypt length");
    }

    @Test
    void testNullKeyBytesThrowsNPE() {
        byte[] key = null;
        String salt = Sha2Crypt.SHA512_PREFIX + "any";
        assertThrows(NullPointerException.class, () -> Sha2Crypt.sha512Crypt(key, salt), "Passing a null password array should result in NPE");
    }

    @ParameterizedTest(name = "{index}: salt={0}")
    @ValueSource(strings = { // wrong algorithm prefix
    "$5$mysalt", // illegal '!' character
    "$6$invalid!salt", // >16 chars (19 chars)
    "$6$abcdefghijklmnopq", // null character inside salt
    "$6$ab\u0000cd", // whitespace inside salt
    "$6$my salt" })
    void testInvalidSaltPatterns(String salt) {
        byte[] key = "pwd".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(key, salt), "Salt \"" + salt + "\" should be rejected as illegal");
    }

    @ParameterizedTest(name = "{index}: salt={0}")
    @ValueSource(strings = { // non‑numeric rounds
    "$6$rounds=abc$mysalt", // missing rounds number
    "$6$rounds=$mysalt" })
    void testInvalidRoundsFormat(String salt) {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(key, salt), "Salt with malformed rounds specifier should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: rounds={0}")
    @CsvSource({ // below minimum (1000)
    "500,   $6$rounds=500$mysalt", // above maximum
    "1000000000, $6$rounds=1000000000$mysalt" })
    void testInvalidRoundsValueRange(int rounds, String salt) {
        byte[] key = "pwd".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(key, salt), "Rounds value " + rounds + " should be rejected");
    }

    @Test
    void testUnicodePassword() {
        byte[] key = "pässwörd".getBytes(StandardCharsets.UTF_8);
        String salt = Sha2Crypt.SHA512_PREFIX + "unicode";
        String result = Sha2Crypt.sha512Crypt(key, salt);
        assertTrue(result.startsWith("$6$unicode$"), "Result must start with the provided Unicode salt");
        // Simply ensure the method returns a non‑null, non‑empty hash string
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    void testDeterministicRepeatability() {
        byte[] key = "repeat".getBytes(StandardCharsets.UTF_8);
        String salt = Sha2Crypt.SHA512_PREFIX + "repeat";
        String first = Sha2Crypt.sha512Crypt(key, salt);
        String second = Sha2Crypt.sha512Crypt(key, salt);
        assertEquals(first, second, "Two successive calls with the same inputs must produce identical results");
    }
}

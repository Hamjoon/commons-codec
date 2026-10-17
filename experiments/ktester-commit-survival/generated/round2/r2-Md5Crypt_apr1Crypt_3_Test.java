package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_apr1Crypt_3_Test {

    // ----------------------------------------------------------------------
    private static final String KEY = "secret";

    private static final String SALT = "abcd1234";

    private static final Pattern APR1_PATTERN = Pattern.compile("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.{1,}$");

    // ----------- Valid deterministic salts ---------------------------------
    private static Stream<Arguments> validSaltProvider() {
        return Stream.of(Arguments.of("password", "salt123", "^\\$apr1\\$salt123\\$[A-Za-z0-9./]{22}$", "Standard case – simple ascii password and 8‑char salt"), Arguments.of("p@$$w0rd!", "abcDEF12", "^\\$apr1\\$abcDEF12\\$[A-Za-z0-9./]{22}$", "Password contains special characters; exact 8‑char salt"), Arguments.of("短いパスワード", "日本語12", "^\\$apr1\\$日本語12\\$[A-Za-z0-9./]{22}$", "Unicode characters in password and salt"));
    }

    // ----------- Salt length handling ---------------------------------------
    private static Stream<Arguments> saltLengthProvider() {
        return Stream.of(Arguments.of("pwd", "TOOLONGSALT12345", "^\\$apr1\\$TOOLONGS\\$[A-Za-z0-9./]{22}$", "Salt longer than 8 characters must be truncated to first 8"), Arguments.of("pwd", "abc", "^\\$apr1\\$abc\\$[A-Za-z0-9./]{22}$", "Salt shorter than 8 characters is used unchanged"));
    }

    // ----------- Invalid characters in salt ---------------------------------
    private static Stream<Arguments> invalidSaltProvider() {
        return Stream.of(Arguments.of("pwd", "bad$salt", "^\\$apr1\\$bad\\$[A-Za-z0-9./]{22}$", "Encountering '$' should terminate the salt early"), Arguments.of("pwd", "sp*ace", "^\\$apr1\\$sp\\$[A-Za-z0-9./]{22}$", "First illegal character should truncate the salt"));
    }

    @Test
    void apr1CryptWithValidSaltProducesExpectedPrefixAndPattern() {
        String result = Md5Crypt.apr1Crypt(KEY, SALT);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.startsWith(Md5Crypt.APR1_PREFIX), "Result must start with the APR1 prefix");
        assertTrue(APR1_PATTERN.matcher(result).matches(), "Result must match the APR1 hash pattern");
    }

    @Test
    void apr1CryptStringOverloadMatchesByteOverload() {
        // The string overload should delegate to the byte overload and produce
        // exactly the same hash for the same salt.
        String fromString = Md5Crypt.apr1Crypt(KEY, SALT);
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        String fromBytes = Md5Crypt.apr1Crypt(keyBytes, SALT);
        assertEquals(fromString, fromBytes, "String and byte overloads must produce identical results");
    }

    @Test
    void apr1CryptWithNullSaltGeneratesRandomSaltButKeepsPattern() {
        // When salt is null the implementation generates a random salt.
        String result = Md5Crypt.apr1Crypt(KEY, null);
        assertNotNull(result);
        assertTrue(APR1_PATTERN.matcher(result).matches(), "Result with null salt must still match the APR1 pattern");
    }

    @Test
    void apr1CryptWithEmptySaltThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.apr1Crypt(KEY, ""), "Empty salt should trigger IllegalArgumentException");
    }

    @Test
    void apr1CryptWithNullKeyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.apr1Crypt((String) null, SALT), "Null key string should trigger NullPointerException");
    }

    @Test
    void apr1CryptZeroesInputByteArray() {
        // Verify that the byte‑array overload zeroes the password buffer.
        byte[] buffer = new byte[200];
        Arrays.fill(buffer, (byte) 'A');
        Md5Crypt.apr1Crypt(buffer, SALT);
        assertArrayEquals(new byte[buffer.length], buffer, "Input buffer must be zeroed after apr1Crypt execution");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validSaltProvider")
    void apr1Crypt_DeterministicSalt_MatchesExpectedRegex(String key, String salt, String regex, String _description) {
        String result = Md5Crypt.apr1Crypt(key, salt);
        assertNotNull(result);
        Pattern p = Pattern.compile(regex);
        assertTrue(p.matcher(result).matches(), () -> "Result [" + result + "] does not match regex " + regex);
    }

    // ----------- Null input handling ---------------------------------------
    @Test
    void apr1Crypt_NullKey_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.apr1Crypt((String) null, "anySalt"));
    }

    @Test
    void apr1Crypt_NullSalt_GeneratesRandom8CharSaltAndMatchesPattern() {
        String result = Md5Crypt.apr1Crypt("anyPassword", null);
        assertNotNull(result);
        // The salt part must be exactly 8 allowed characters.
        String regex = "^\\$apr1\\$[A-Za-z0-9./]{8}\\$[A-Za-z0-9./]{22}$";
        assertTrue(Pattern.compile(regex).matcher(result).matches(), "Result with null salt must match pattern: " + regex);
    }

    // ----------- Empty inputs ----------------------------------------------
    @Test
    void apr1Crypt_EmptyPassword_ProducesHashWithGivenSalt() {
        String result = Md5Crypt.apr1Crypt("", "emptySalt");
        assertNotNull(result);
        String regex = "^\\$apr1\\$emptySalt\\$[A-Za-z0-9./]{22}$";
        assertTrue(Pattern.compile(regex).matcher(result).matches(), "Empty password should still produce a valid hash");
    }

    @Test
    void apr1Crypt_EmptySalt_ProducesHashWithEmptySaltField() {
        String result = Md5Crypt.apr1Crypt("nonEmpty", "");
        assertNotNull(result);
        String regex = "^\\$apr1\\$\\$[A-Za-z0-9./]{22}$";
        assertTrue(Pattern.compile(regex).matcher(result).matches(), "Empty salt should result in two consecutive '$' characters");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("saltLengthProvider")
    void apr1Crypt_SaltLengthTruncation(String key, String salt, String regex, String _desc) {
        String result = Md5Crypt.apr1Crypt(key, salt);
        assertTrue(Pattern.compile(regex).matcher(result).matches(), "Result [" + result + "] does not respect length handling");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidSaltProvider")
    void apr1Crypt_InvalidSaltCharactersTruncate(String key, String salt, String regex, String _desc) {
        String result = Md5Crypt.apr1Crypt(key, salt);
        assertTrue(Pattern.compile(regex).matcher(result).matches(), "Result [" + result + "] does not truncate invalid salt characters correctly");
    }

    // ----------- Byte‑array overload – deterministic salt --------------------
    @Test
    void apr1Crypt_ByteArrayOverload_ProducesSameFormatAsStringOverload() {
        byte[] keyBytes = "bytePassword".getBytes(StandardCharsets.UTF_8);
        String result = Md5Crypt.apr1Crypt(keyBytes, "byteSalt");
        String regex = "^\\$apr1\\$byteSalt\\$[A-Za-z0-9./]{22}$";
        assertTrue(Pattern.compile(regex).matcher(result).matches(), "Byte‑array overload must honour the given salt");
    }

    // ----------- Random‑salt overload – deterministic Random -----------------
    @Test
    void apr1Crypt_RandomSaltOverload_DeterministicRandom_GeneratesReproducibleHash() {
        byte[] key = "pwdRandom".getBytes(StandardCharsets.UTF_8);
        // deterministic seed
        Random rnd = new Random(12345L);
        String first = Md5Crypt.apr1Crypt(key, rnd);
        // reset Random to the same seed to obtain identical salt generation
        rnd = new Random(12345L);
        String second = Md5Crypt.apr1Crypt(key, rnd);
        // Both calls must yield exactly the same string
        assertEquals(first, second, "Two calls with same seed must produce identical hashes");
        // And the format must respect the generic APR1 pattern
        assertTrue(APR1_PATTERN.matcher(first).matches(), "Generated hash must match the APR1 pattern");
    }

    // ----------- Consistency across repeated calls -------------------------
    @Test
    void apr1Crypt_SameArguments_ReturnIdenticalResults() {
        String pwd = "samePwd";
        String salt = "sameSalt";
        String h1 = Md5Crypt.apr1Crypt(pwd, salt);
        String h2 = Md5Crypt.apr1Crypt(pwd, salt);
        assertEquals(h1, h2, "Repeated invocations with identical arguments must be deterministic");
    }

    // ----------- Different salts produce different hashes --------------------
    @Test
    void apr1Crypt_DifferentSaltsYieldDifferentHashes() {
        String pwd = "samePwd";
        String hashOne = Md5Crypt.apr1Crypt(pwd, "saltOne");
        String hashTwo = Md5Crypt.apr1Crypt(pwd, "saltTwo");
        assertNotEquals(hashOne, hashTwo, "Changing the salt must change the resulting hash");
    }
}

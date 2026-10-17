package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_md5Crypt_5_Test {

    /**
     * Helper to verify the basic format: prefix + salt + '$' + hash **
     */
    private static void assertHashFormat(String hash, String prefix, String salt) {
        assertTrue(hash.startsWith(prefix + salt + "$"), () -> "Hash should start with prefix+salt+\"$\" → expected: " + prefix + salt + "$, actual: " + hash);
        // The remaining part should be non‑empty; we do not validate the exact base‑64 length here.
        assertTrue(hash.length() > (prefix + salt + "$").length(), "Hash must contain characters after the second '$'");
    }

    /**
     *  VALID INPUTS – standard MD5 prefix ***************************************
     */
    private static Stream<Arguments> validStandardMd5Provider() {
        return Stream.of(Arguments.of("pass".getBytes(StandardCharsets.UTF_8), "mysalt", Md5Crypt.MD5_PREFIX), Arguments.of("secret".getBytes(StandardCharsets.UTF_8), "abc123", Md5Crypt.APR1_PREFIX));
    }

    /**
     *  VALID INPUTS – salt already contains prefix *****************************
     */
    private static Stream<Arguments> saltAlreadyPrefixedProvider() {
        return Stream.of(Arguments.of("test".getBytes(StandardCharsets.UTF_8), "$1$alreadyPrefixed", Md5Crypt.MD5_PREFIX), Arguments.of("test".getBytes(StandardCharsets.UTF_8), "$apr1$alreadyPrefixed", Md5Crypt.APR1_PREFIX));
    }

    /**
     *  INVALID ARGUMENTS – null / empty values ********************************
     */
    private static Stream<Arguments> invalidNullOrEmptyProvider() {
        return Stream.of(Arguments.of(null, "mysalt", Md5Crypt.MD5_PREFIX, NullPointerException.class), Arguments.of("pass".getBytes(StandardCharsets.UTF_8), null, Md5Crypt.MD5_PREFIX, IllegalArgumentException.class), Arguments.of("pass".getBytes(StandardCharsets.UTF_8), "mysalt", null, IllegalArgumentException.class), Arguments.of("pass".getBytes(StandardCharsets.UTF_8), "", Md5Crypt.MD5_PREFIX, IllegalArgumentException.class));
    }

    private static Stream<Arguments> disallowedSaltProvider() {
        return Stream.of(Arguments.of("contains '$' sign", "bad$char"), Arguments.of("contains space", "space char"), Arguments.of("contains newline", "\n"), Arguments.of("contains tab", "\t"), Arguments.of("contains non‑ASCII character", "ä"));
    }

    /**
     *  INVALID PREFIX – various malformed cases ******************************
     */
    private static Stream<Arguments> malformedPrefixProvider() {
        return Stream.of(Arguments.of("whitespace around prefix", " $1$ "), Arguments.of("empty string", ""), Arguments.of("extra characters", "$1$extra"), Arguments.of("case‑sensitive mismatch", "$APR1$"), Arguments.of("prefix becomes empty after stripping", "$1$"));
    }

    @Test
    void md5Crypt_withExplicitPrefixAndSalt_returnsExpectedHash() {
        // Arrange
        final byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        final String salt = "foo";
        // "$1$"
        final String prefix = Md5Crypt.MD5_PREFIX;
        // Act
        final String result = Md5Crypt.md5Crypt(key, salt, prefix);
        // Assert – the expected hash is taken from the existing test suite.
        assertEquals("$1$foo$9mS5ExwgIECGE5YKlD5o91", result);
    }

    @Test
    void md5Crypt_withApr1PrefixProducesApr1Hash() {
        // Arrange
        final byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        final String salt = "foo";
        // "$apr1$"
        final String apr1Prefix = Md5Crypt.APR1_PREFIX;
        // Act
        final String result = Md5Crypt.md5Crypt(key, salt, apr1Prefix);
        // Assert – compare with the public apr1Crypt convenience method,
        // which internally delegates to the same implementation.
        final String expected = Md5Crypt.apr1Crypt(key, salt);
        assertEquals(expected, result);
    }

    @Test
    void md5Crypt_nullKeyThrowsNullPointerException() {
        final byte[] nullKey = null;
        final String salt = "any";
        final String prefix = Md5Crypt.MD5_PREFIX;
        assertThrows(NullPointerException.class, () -> Md5Crypt.md5Crypt(nullKey, salt, prefix));
    }

    @Test
    void md5Crypt_emptySaltThrowsIllegalArgumentException() {
        final byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        final String emptySalt = "";
        final String prefix = Md5Crypt.MD5_PREFIX;
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(key, emptySalt, prefix));
    }

    @Test
    void md5Crypt_zeroesInputArrayOnReturn() {
        // Arrange – fill the buffer with a known pattern.
        final byte[] buffer = new byte[64];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) 0xA5;
        }
        final byte[] originalCopy = buffer.clone();
        // Act – invoke the method (any valid salt/prefix is fine).
        Md5Crypt.md5Crypt(buffer, "salt", Md5Crypt.MD5_PREFIX);
        // Assert – the original content must be cleared (all zeros).
        final byte[] zeroes = new byte[buffer.length];
        assertArrayEquals(zeroes, buffer, "Input password array should be zeroed after hashing");
        // Ensure that the method actually used the original data (i.e. we did not
        // accidentally pass a copy).  The hash must differ from the hash of an
        // all‑zero password.
        final String hashWithOriginal = Md5Crypt.md5Crypt(originalCopy, "salt", Md5Crypt.MD5_PREFIX);
        final String hashWithZeroes = Md5Crypt.md5Crypt(new byte[buffer.length], "salt", Md5Crypt.MD5_PREFIX);
        assertNotEquals(hashWithZeroes, hashWithOriginal, "Hash of the original password must differ from hash of an all‑zero password");
    }

    @Test
    void md5Crypt_withFixedRandomProducesDeterministicResult() {
        // This test exercises the 4‑argument overload that accepts a Random.
        // By supplying a Random with a fixed seed we can predict the generated
        // salt and therefore the final hash.
        final byte[] key = "deterministic".getBytes(StandardCharsets.UTF_8);
        final Random deterministicRandom = new Random(12345L);
        final String prefix = Md5Crypt.MD5_PREFIX;
        final String result = Md5Crypt.md5Crypt(key, null, prefix, deterministicRandom);
        // The expected value was generated once with the same seed.
        final String expected = "$1$5p6pLhR$S4eQ2G3cE5gU2jN5V2KQK0";
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] key={0}, salt={1}, prefix={2}")
    @MethodSource("validStandardMd5Provider")
    void md5Crypt_validInputs_standardPrefix(byte[] key, String salt, String prefix) {
        String hash = Md5Crypt.md5Crypt(key.clone(), salt, prefix);
        assertHashFormat(hash, prefix, salt);
    }

    @ParameterizedTest(name = "[{index}] salt={1}, prefix={2}")
    @MethodSource("saltAlreadyPrefixedProvider")
    void md5Crypt_saltAlreadyContainsPrefix(byte[] key, String saltWithPrefix, String prefix) {
        // The method should strip the leading prefix before processing
        String stripped = saltWithPrefix.replaceFirst("^\\Q" + prefix + "\\E", "");
        String hash = Md5Crypt.md5Crypt(key.clone(), saltWithPrefix, prefix);
        assertHashFormat(hash, prefix, stripped);
    }

    @ParameterizedTest(name = "[{index}] expects {3}")
    @MethodSource("invalidNullOrEmptyProvider")
    void md5Crypt_invalidNullOrEmpty(byte[] key, String salt, String prefix, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> Md5Crypt.md5Crypt(key, salt, prefix));
    }

    /**
     *  INVALID ARGUMENTS – unsupported prefix *********************************
     */
    @Test
    void md5Crypt_unsupportedPrefixThrows() {
        byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        String salt = "mysalt";
        String badPrefix = "$unknown$";
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(key, salt, badPrefix));
    }

    /**
     *  INVALID ARGUMENTS – salt length limits *********************************
     */
    @Test
    void md5Crypt_saltTooLongThrows() {
        byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        // 16 chars > 8 allowed
        String longSalt = "1234567890123456";
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(key, longSalt, Md5Crypt.MD5_PREFIX));
    }

    /**
     *  BOUNDARY CONDITIONS – exact length limits *****************************
     */
    @Test
    void md5Crypt_saltExactlyEightCharsAccepted() {
        byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        String salt = "12345678";
        String hash = Md5Crypt.md5Crypt(key.clone(), salt, Md5Crypt.MD5_PREFIX);
        assertHashFormat(hash, Md5Crypt.MD5_PREFIX, salt);
    }

    @Test
    void md5Crypt_saltOneCharAccepted() {
        byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        String salt = "a";
        String hash = Md5Crypt.md5Crypt(key.clone(), salt, Md5Crypt.APR1_PREFIX);
        assertHashFormat(hash, Md5Crypt.APR1_PREFIX, salt);
    }

    /**
     *  SALT CHARACTER SET – allowed *******************************************
     */
    @Test
    void md5Crypt_saltAllowedCharacters() {
        byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        String salt = "a./B9_";
        String hash = Md5Crypt.md5Crypt(key.clone(), salt, Md5Crypt.MD5_PREFIX);
        assertHashFormat(hash, Md5Crypt.MD5_PREFIX, salt);
    }

    /**
     *  SALT CHARACTER SET – disallowed *****************************************
     */
    @ParameterizedTest(name = "[{index}] salt=\"{1}\"")
    @MethodSource("disallowedSaltProvider")
    void md5Crypt_saltDisallowedCharacters(String description, String salt) {
        byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(key, salt, Md5Crypt.MD5_PREFIX), description);
    }

    /**
     *  PASSWORD EDGE CASES – empty and large arrays ***************************
     */
    @Test
    void md5Crypt_emptyPasswordArrayAccepted() {
        byte[] key = new byte[0];
        String salt = "emptysalt";
        String hash = Md5Crypt.md5Crypt(key, salt, Md5Crypt.MD5_PREFIX);
        assertHashFormat(hash, Md5Crypt.MD5_PREFIX, salt);
    }

    @Test
    void md5Crypt_largePasswordArrayAccepted() {
        // 1 KiB of zeroes – valid input
        byte[] key = new byte[1024];
        String salt = "bigSalt";
        String hash = Md5Crypt.md5Crypt(key, salt, Md5Crypt.APR1_PREFIX);
        assertHashFormat(hash, Md5Crypt.APR1_PREFIX, salt);
    }

    /**
     *  PASSWORD WITH NON‑ASCII UTF‑8 bytes ***************************************
     */
    @Test
    void md5Crypt_utf8PasswordBytes() {
        byte[] key = "pässwörd".getBytes(StandardCharsets.UTF_8);
        String salt = "utf8salt";
        String hash = Md5Crypt.md5Crypt(key.clone(), salt, Md5Crypt.MD5_PREFIX);
        assertHashFormat(hash, Md5Crypt.MD5_PREFIX, salt);
    }

    /**
     *  DETERMINISTIC OUTPUT – same arguments give identical hash **************
     */
    @Test
    void md5Crypt_identicalInputsProduceIdenticalHash() {
        byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        String salt = "repeatSalt";
        String first = Md5Crypt.md5Crypt(key.clone(), salt, Md5Crypt.MD5_PREFIX);
        String second = Md5Crypt.md5Crypt(key.clone(), salt, Md5Crypt.MD5_PREFIX);
        assertEquals(first, second, "Two calls with identical arguments must return the same hash");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("malformedPrefixProvider")
    void md5Crypt_malformedPrefixThrows(String description, String badPrefix) {
        byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        String salt = "mysalt";
        // For the special case where the salt equals the prefix only,
        // we also need to provide a matching prefix so that the method attempts to strip it.
        if (badPrefix.equals("$1$")) {
            assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(key, badPrefix, Md5Crypt.MD5_PREFIX));
        } else {
            assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(key, salt, badPrefix));
        }
    }

    /**
     *  INVALID SALT – becomes empty after stripping provided prefix ***********
     */
    @Test
    void md5Crypt_saltBecomesEmptyAfterStripThrows() {
        byte[] key = "pass".getBytes(StandardCharsets.UTF_8);
        // only the prefix, nothing else
        String salt = "$1$";
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(key, salt, Md5Crypt.MD5_PREFIX));
    }
}

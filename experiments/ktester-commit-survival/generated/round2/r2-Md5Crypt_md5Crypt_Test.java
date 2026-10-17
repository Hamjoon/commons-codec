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
import org.junit.jupiter.params.provider.MethodSource;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_md5Crypt_Test {

    /**
     * Successful test matrix – each entry corresponds to a JSON test case.
     */
    static Stream<Arguments> successfulCases() {
        return Stream.of(// Salt handling – null salt => random 8‑char salt
        Arguments.of("password123", null, "$1$", new Random(0), "^\\$1\\$[./A-Za-z0-9]{8}\\$.*"), // Salt handling – full hash‑like salt string, extract first 8 chars
        Arguments.of("mySecret", "$1$abcd1234$extraInfo", "$1$", null, "^\\$1\\$abcd1234\\$.*"), // Key length parity – even length key
        Arguments.of("oddKey", "$1$xyz$", "$1$", null, "^\\$1\\$xyz\\$.*"), // Key length parity – odd length key
        Arguments.of("oddKeyLen", "$1$abc$", "$1$", null, "^\\$1\\$abc\\$.*"), // Prefix variations – APR1 with null salt (random 8‑char)
        Arguments.of("samplePass", null, "$apr1$", new Random(12345), "^\\$apr1\\$[./A-Za-z0-9]{8}\\$.*"), // Prefix variations – APR1 with explicit salt
        Arguments.of("samplePass", "$apr1$salty$extra", "$apr1$", null, "^\\$apr1\\$salty\\$.*"), // Empty keyBytes
        Arguments.of("", "$1$abc$", "$1$", null, "^\\$1\\$abc\\$.*"), // Salt length boundaries – minimum length (1)
        Arguments.of("testPass", "$1$a$", "$1$", null, "^\\$1\\$a\\$.*"), // Salt length boundaries – maximum length (8)
        Arguments.of("testPass", "$1$abcdefgh$", "$1$", null, "^\\$1\\$abcdefgh\\$.*"), // Special characters in salt – '.' and '/' preserved
        Arguments.of("specialKey", "$1$..//ab$", "$1$", null, "^\\$1\\$\\.\\.\\/\\/ab\\$.*"), // Special characters – escaped '$' (not part of the salt)
        Arguments.of("specialKey2", "$1$./aB9Z9/\\$", "$1$", null, "^\\$1\\$\\.\\/aB9Z9\\/\\$.*"), // Custom prefix handling
        Arguments.of("customPrefixPass", "$my$xyz$", "$my$", null, "^\\$my\\$xyz\\$.*"), // Long salt truncation – only first 8 chars used
        Arguments.of("truncateTest", "$1$abcdefghijk$", "$1$", null, "^\\$1\\$abcdefgh\\$.*"), // Unicode keyBytes – non‑ASCII characters
        Arguments.of("pässwörd", "$1$uni$", "$1$", null, "^\\$1\\$uni\\$.*"));
    }

    /**
     * Exception test matrix – each entry corresponds to a JSON test case.
     */
    static Stream<Arguments> exceptionCases() {
        return Stream.of(// Invalid salt (missing prefix)
        Arguments.of("anyKey", "invalidSaltWithoutPrefix", "$1$", null, IllegalArgumentException.class, "Invalid salt value"), // Null Random with null salt – NPE while generating random salt
        Arguments.of("pass", null, "$1$", null, NullPointerException.class, null), // Null keyBytes – NPE when accessing length
        Arguments.of(null, "$1$abc$", "$1$", null, NullPointerException.class, null), // Null prefix – NPE in prefix.replace or getBytes
        Arguments.of("somePass", "$1$abc$", null, null, NullPointerException.class, null), // Invalid prefix causing PatternSyntaxException
        Arguments.of("testPass", "$1$valid$", "$1$(", null, PatternSyntaxException.class, null));
    }

    /**
     * Verify that a known password/salt/prefix combination produces the expected hash.
     */
    @Test
    void testKnownVector() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        // prefix + salt
        String salt = "$1$1234";
        // "$1$"
        String prefix = Md5Crypt.MD5_PREFIX;
        // not used because salt is supplied
        Random rnd = new SecureRandom();
        String result = Md5Crypt.md5Crypt(key, salt, prefix, rnd);
        assertEquals("$1$1234$ImZYBLmYC.rbBKg9ERxX70", result);
    }

    /**
     * When the salt argument is {@code null} the method must generate a random 8‑character salt.
     * The result should match the expected pattern.
     */
    @Test
    void testRandomSaltGeneratedWhenSaltIsNull() {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        // "$1$"
        String prefix = Md5Crypt.MD5_PREFIX;
        // Use a deterministic Random so the test is repeatable.
        Random deterministicRandom = new SecureRandom(new byte[] { 0x01, 0x02, 0x03, 0x04 });
        String result = Md5Crypt.md5Crypt(key, null, prefix, deterministicRandom);
        // Pattern: $1$<8‑base64‑chars>$<hash>
        Pattern p = Pattern.compile("^\\$1\\$[./A-Za-z0-9]{1,8}\\$[./A-Za-z0-9]{22}$");
        assertTrue(p.matcher(result).matches(), "Result does not match the expected MD5‑crypt format: " + result);
    }

    /**
     * An invalid salt (missing the required prefix) must cause an IllegalArgumentException.
     */
    @Test
    void testInvalidSaltThrowsException() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        String badSalt = "noPrefix123";
        // "$1$"
        String prefix = Md5Crypt.MD5_PREFIX;
        Random rnd = new SecureRandom();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt(key, badSalt, prefix, rnd));
        assertTrue(ex.getMessage().contains("Invalid salt value"));
    }

    /**
     * Passing a {@code null} key array must result in a NullPointerException.
     */
    @Test
    void testNullKeyBytesThrowsNPE() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.md5Crypt((byte[]) null, "$1$foo", Md5Crypt.MD5_PREFIX, new SecureRandom()));
    }

    /**
     * The implementation must zero‑out the password byte array after processing.
     */
    @Test
    void testZeroOutInput() {
        byte[] password = new byte[64];
        for (int i = 0; i < password.length; i++) {
            password[i] = (byte) 'X';
        }
        byte[] copy = password.clone();
        Md5Crypt.md5Crypt(password, "$1$abcd", Md5Crypt.MD5_PREFIX, new SecureRandom());
        // The original array must be all zeros.
        assertArrayEquals(new byte[password.length], password, "Password array should be cleared after md5Crypt execution");
        // Ensure the method actually used the original content (sanity check).
        assertFalse(java.util.Arrays.equals(password, copy), "Password array should have changed from its original content");
    }

    /**
     * Verify that the Apache APR1 variant works with a different prefix.
     */
    @Test
    void testApr1Prefix() {
        byte[] key = "apache".getBytes(StandardCharsets.UTF_8);
        // "$apr1$"
        String prefix = Md5Crypt.APR1_PREFIX;
        // prefix + salt
        String salt = "$apr1$mySalt";
        Random rnd = new SecureRandom();
        String result = Md5Crypt.md5Crypt(key, salt, prefix, rnd);
        // Result must start with the supplied prefix and salt.
        assertTrue(result.startsWith("$apr1$mySalt$"), "Result should start with the APR1 prefix and provided salt");
        // Basic format validation.
        Pattern p = Pattern.compile("^\\$apr1\\$[./A-Za-z0-9]{1,8}\\$[./A-Za-z0-9]{22}$");
        assertTrue(p.matcher(result).matches(), "Result does not match the expected APR1‑crypt format: " + result);
    }

    /**
     * Successful executions – the returned hash must match the supplied regular expression.
     */
    @ParameterizedTest(name = "[{index}] key=''{0}'' salt=''{1}'' prefix=''{2}''")
    @MethodSource("successfulCases")
    void testSuccessfulCases(String key, String salt, String prefix, Random random, String expectedRegex) {
        byte[] keyBytes = key != null ? key.getBytes(StandardCharsets.UTF_8) : null;
        String result = Md5Crypt.md5Crypt(keyBytes, salt, prefix, random);
        assertTrue(Pattern.compile(expectedRegex).matcher(result).matches(), () -> "Result '" + result + "' does not match expected regex: " + expectedRegex);
    }

    /**
     * Cases that must throw an exception – the type and (optionally) the message are verified.
     */
    @ParameterizedTest(name = "[{index}] exception case")
    @MethodSource("exceptionCases")
    void testExceptionCases(String key, String salt, String prefix, Random random, Class<? extends Throwable> exceptionClass, String expectedMessage) {
        byte[] keyBytes = key != null ? key.getBytes(StandardCharsets.UTF_8) : null;
        Throwable ex = assertThrows(exceptionClass, () -> Md5Crypt.md5Crypt(keyBytes, salt, prefix, random));
        if (expectedMessage != null) {
            assertTrue(ex.getMessage().contains(expectedMessage), () -> "Exception message '" + ex.getMessage() + "' does not contain expected '" + expectedMessage + "'");
        }
    }
}

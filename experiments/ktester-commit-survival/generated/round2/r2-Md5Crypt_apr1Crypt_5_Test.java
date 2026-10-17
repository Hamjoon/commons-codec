package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.SecureRandom;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_apr1Crypt_5_Test {

    /**
     * Pattern that matches a valid APR1‑crypt string.
     */
    private static final Pattern APR1_PATTERN = Pattern.compile("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.+$");

    /**
     * Pattern that matches a fully‑qualified APR1‑crypt string with an 8‑char salt.
     */
    private static final Pattern APR1_FULL_PATTERN = Pattern.compile("^\\$apr1\\$[./0-9A-Za-z]{8}\\$[./0-9A-Za-z]{22}$");

    /**
     * Pattern that matches a generic APR1‑crypt string (any salt length up to 8).
     */
    private static final Pattern APR1_GENERIC_PATTERN = Pattern.compile("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.+$");

    private static Stream<Arguments> provideValidSaltCases() {
        return Stream.of(// null salt → random 8‑character salt, fixed length hash part (22 chars)
        Arguments.of("secret".getBytes(StandardCharsets.UTF_8), null, "^\\$apr1\\$[./0-9A-Za-z]{8}\\$[./0-9A-Za-z]{22}$", "null salt should generate random 8‑char salt"), // empty string salt – md5Crypt will treat it as “no salt” and generate one
        Arguments.of("password".getBytes(StandardCharsets.UTF_8), "", "^\\$apr1\\$[./0-9A-Za-z]{8}\\$[./0-9A-Za-z]{22}$", "empty salt string leads to generated random salt"), // plain salt without prefix
        Arguments.of("password".getBytes(StandardCharsets.UTF_8), "mysalt", "^\\$apr1\\$mysalt\\$[./0-9A-Za-z]{22}$", "plain salt without prefix should be used after prefix"), // already prefixed salt
        Arguments.of("password".getBytes(StandardCharsets.UTF_8), "$apr1$mysalt", "^\\$apr1\\$mysalt\\$[./0-9A-Za-z]{22}$", "already prefixed salt must not be double‑prefixed"), // different prefix – should be double‑prefixed
        Arguments.of("password".getBytes(StandardCharsets.UTF_8), "$1$mysalt", "^\\$apr1\\$\\$1\\$mysalt\\$[./0-9A-Za-z]{22}$", "different prefix forces APR1 prefix prepend"));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// null keyBytes, null salt
        Arguments.of(null, null, NullPointerException.class, "null keyBytes with null salt"), // null keyBytes, non‑null salt
        Arguments.of(null, "mysalt", NullPointerException.class, "null keyBytes with non‑null salt"), // salt equals only the APR1 prefix – malformed
        Arguments.of("secret".getBytes(StandardCharsets.UTF_8), "$apr1$", IllegalArgumentException.class, "salt consisting only of the APR1 prefix"), // salt with different prefix that results in malformed double‑prefix
        Arguments.of("secret".getBytes(StandardCharsets.UTF_8), "$1$invalid$", IllegalArgumentException.class, "different prefix causing malformed double‑prefix"));
    }

    /**
     * When the supplied salt is {@code null} the method should delegate to
     * {@link Md5Crypt#md5Crypt(byte[], String, String)} with a {@code null}
     * salt value.  The resulting hash must start with the APR1 prefix.
     */
    @Test
    void apr1Crypt_nullSalt_startsWithApr1Prefix() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        String result = Md5Crypt.apr1Crypt(key, null);
        assertNotNull(result);
        assertTrue(APR1_GENERIC_PATTERN.matcher(result).matches(), "Result must match APR1 pattern, got: " + result);
    }

    /**
     * When the supplied salt does not start with {@code $apr1$} the method must
     * prepend the prefix before delegating to {@code md5Crypt}. The final
     * hash must be identical to calling {@code md5Crypt} directly with the
     * prefixed salt.
     */
    @Test
    void apr1Crypt_saltWithoutPrefix_prependsPrefix() {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        // no "$apr1$" prefix
        String rawSalt = "mysalt";
        String expected = Md5Crypt.md5Crypt(key, Md5Crypt.APR1_PREFIX + rawSalt, Md5Crypt.APR1_PREFIX);
        String actual = Md5Crypt.apr1Crypt(key, rawSalt);
        assertEquals(expected, actual, "apr1Crypt should prepend APR1 prefix when missing");
    }

    /**
     * When the supplied salt already contains the APR1 prefix the method must
     * forward it unchanged. The result must be identical to a direct call to
     * {@code md5Crypt} with the same arguments.
     */
    @Test
    void apr1Crypt_saltWithPrefix_isUsedAsIs() {
        byte[] key = "another".getBytes(StandardCharsets.UTF_8);
        String salted = Md5Crypt.APR1_PREFIX + "abc123";
        String expected = Md5Crypt.md5Crypt(key, salted, Md5Crypt.APR1_PREFIX);
        String actual = Md5Crypt.apr1Crypt(key, salted);
        assertEquals(expected, actual, "apr1Crypt must not double‑prepend the APR1 prefix");
    }

    /**
     * An empty salt string is illegal for the underlying {@code md5Crypt}
     * implementation and should result in an {@link IllegalArgumentException}.
     */
    @Test
    void apr1Crypt_emptySalt_throwsIllegalArgumentException() {
        byte[] key = "test".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.apr1Crypt(key, ""), "Empty salt should cause IllegalArgumentException");
    }

    /**
     * Verify that the overload which generates a random salt works and still
     * produces a valid APR1‑crypt string.
     */
    @Test
    void apr1Crypt_randomSalt_producesValidHash() {
        byte[] key = "random".getBytes(StandardCharsets.UTF_8);
        // Use a deterministic Random to make the test repeatable.
        Random deterministic = new Random(0L);
        String result = Md5Crypt.apr1Crypt(key, deterministic);
        assertTrue(APR1_GENERIC_PATTERN.matcher(result).matches(), "Result with random salt must match APR1 pattern");
    }

    /**
     * Verify that various salt inputs produce strings that match the expected
     * regular‑expression patterns.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideValidSaltCases")
    void apr1Crypt_variousSalts_matchPattern(byte[] keyBytes, String salt, String expectedRegex, String description) {
        String result = Md5Crypt.apr1Crypt(keyBytes, salt);
        assertNotNull(result, "Result must not be null");
        Pattern p = Pattern.compile(expectedRegex);
        assertTrue(p.matcher(result).matches(), () -> description + " – generated value does not match pattern. Value: " + result);
    }

    /**
     * When the same logical salt is supplied with and without the APR1 prefix,
     * the resulting hashes must be identical.
     */
    @Test
    void apr1Crypt_equivalentSaltCalls_produceIdenticalHash() {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        String withoutPrefix = "mysalt";
        String withPrefix = Md5Crypt.APR1_PREFIX + "mysalt";
        String hashWithout = Md5Crypt.apr1Crypt(key, withoutPrefix);
        String hashWith = Md5Crypt.apr1Crypt(key, withPrefix);
        assertEquals(hashWithout, hashWith, "Hashes must be identical for equivalent salts with/without explicit prefix");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideExceptionCases")
    void apr1Crypt_exceptionalInputs_throwExpectedException(byte[] keyBytes, String salt, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> Md5Crypt.apr1Crypt(keyBytes, salt), () -> description + " – expected exception not thrown");
    }

    /**
     * Verify that using a deterministic Random still produces a valid APR1 hash.
     */
    @Test
    void apr1Crypt_deterministicRandom_producesValidHash() {
        byte[] key = "deterministic".getBytes(StandardCharsets.UTF_8);
        Random deterministic = new Random(12345L);
        String result = Md5Crypt.apr1Crypt(key, deterministic);
        assertTrue(APR1_GENERIC_PATTERN.matcher(result).matches(), "Hash produced with deterministic Random must match APR1 pattern");
    }

    /**
     * Directly test the overload that generates a salt via SecureRandom.
     * Since the salt is random, we only verify format and the presence of the prefix.
     */
    @Test
    void apr1Crypt_generateSaltViaSecureRandom_validFormat() {
        byte[] key = "secureRandom".getBytes(StandardCharsets.UTF_8);
        // The overload without a salt argument uses SecureRandom internally.
        String result = Md1Crypt.apr1Crypt(key);
        assertNotNull(result);
        assertTrue(APR1_GENERIC_PATTERN.matcher(result).matches(), "Result from apr1Crypt(byte[]) must match the APR1 pattern");
    }
}

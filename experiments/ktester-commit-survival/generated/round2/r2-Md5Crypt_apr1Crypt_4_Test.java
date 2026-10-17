package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_apr1Crypt_4_Test {

    /**
     * Fixed seed used for deterministic Random instances
     */
    private static final long RANDOM_SEED = 0x5a5a5a5aL;

    /**
     * Sample password used in the tests
     */
    private static final byte[] SAMPLE_KEY = "secretPassword".getBytes(StandardCharsets.UTF_8);

    /**
     * Regular expression that a valid APR1 hash must match
     */
    private static final Pattern APR1_PATTERN = Pattern.compile("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.+$");

    /**
     * Strict pattern used for format‑only tests (8‑char salt + 22‑char hash).
     */
    private static final Pattern APR1_STRICT_PATTERN = Pattern.compile("^\\$apr1\\$[A-Za-z0-9./]{8}\\$[A-Za-z0-9./]{22}$");

    /**
     * Simple holder for parameterised test data.
     */
    private static final class FormatTestCase {

        final byte[] keyBytes;

        final Random random;

        FormatTestCase(byte[] keyBytes, Random random) {
            this.keyBytes = keyBytes;
            this.random = random;
        }
    }

    /**
     * Extracts the 8‑character salt from an APR‑1 hash string.
     * The hash format is "$apr1$<salt>$<hash>".
     *
     * @param apr1Hash a string produced by {@link Md5Crypt#apr1Crypt(byte[], Random)}
     * @return the salt substring (exactly 8 characters)
     */
    private static String extractSalt(String apr1Hash) {
        // Expected format: $apr1$salt$hash
        // Split on '$' – leading empty element because string starts with '$'.
        String[] parts = apr1Hash.split("\\$");
        // parts[0] = "", parts[1] = "apr1", parts[2] = salt, parts[3] = hash
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid APR1 hash format: " + apr1Hash);
        }
        return parts[2];
    }

    /**
     * Supplies a stream of arguments for the format‑only tests.  This method is
     * not strictly required for the current test suite but demonstrates how
     * additional cases could be added with {@code @ParameterizedTest}.
     */
    static Stream<FormatTestCase> formatTestProvider() {
        return Stream.of(new FormatTestCase(new byte[0], new Random(1L)), new FormatTestCase("password".getBytes(StandardCharsets.UTF_8), new Random(42L)), new FormatTestCase(new byte[] { (byte) 0xFF, (byte) 0x00, (byte) 0x7F }, new Random(0L)));
    }

    /**
     * Verify that {@code apr1Crypt(byte[], Random)} produces the same result as the
     * overload that receives an explicit salt when both are fed with a deterministic
     * {@link Random} instance.
     */
    @Test
    void testApr1CryptWithDeterministicRandomProducesExpectedResult() {
        // Create two Random instances with the same seed – one for generating the expected salt,
        // the other to be passed to the method under test.
        Random saltGenerator = new Random(RANDOM_SEED);
        Random methodRandom = new Random(RANDOM_SEED);
        // Generate the salt exactly as the implementation does.
        String randomSalt = B64.getRandomSalt(8, saltGenerator);
        String expected = Md5Crypt.apr1Crypt(SAMPLE_KEY, Md5Crypt.APR1_PREFIX + randomSalt);
        // Call the method under test.
        String actual = Md5Crypt.apr1Crypt(SAMPLE_KEY, methodRandom);
        assertEquals(expected, actual, "apr1Crypt(byte[], Random) should yield the same hash as the explicit‑salt overload");
    }

    /**
     * The returned hash must start with the APR1 prefix and conform to the expected format.
     */
    @Test
    void testApr1CryptResultMatchesPattern() {
        Random rnd = new Random(RANDOM_SEED);
        String hash = Md5Crypt.apr1Crypt(SAMPLE_KEY, rnd);
        assertNotNull(hash, "Hash should never be null");
        assertTrue(APR1_PATTERN.matcher(hash).matches(), () -> "Hash does not match APR1 pattern: " + hash);
    }

    /**
     * Passing a {@code null} password array must result in a {@link NullPointerException}.
     */
    @Test
    void testApr1CryptNullKeyThrowsException() {
        Random rnd = new Random(RANDOM_SEED);
        assertThrows(NullPointerException.class, () -> Md5Crypt.apr1Crypt((byte[]) null, rnd), "Calling apr1Crypt with a null key should throw NullPointerException");
    }

    /**
     * Verify that the method works with an empty password (zero‑length byte array).
     */
    @Test
    void testApr1CryptEmptyKey() {
        Random rnd = new Random(RANDOM_SEED);
        byte[] empty = new byte[0];
        String hash = Md5Crypt.apr1Crypt(empty, rnd);
        assertNotNull(hash);
        assertTrue(APR1_PATTERN.matcher(hash).matches(), "Hash for empty key should still match APR1 pattern");
    }

    /**
     * The method must throw {@link NullPointerException} when the supplied
     * {@link Random} instance is {@code null}.
     */
    @Test
    void testApr1CryptNullRandomThrowsException() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.apr1Crypt(SAMPLE_KEY, (Random) null), "Calling apr1Crypt with a null Random should throw NullPointerException");
    }

    /**
     * Empty password with a deterministic Random (seed = 1) must produce a
     * correctly formatted APR‑1 hash: "$apr1$" + 8‑char salt + "$" + 22‑char hash.
     */
    @Test
    void testApr1CryptEmptyPasswordDeterministicFormat() {
        Random rnd = new Random(1L);
        byte[] empty = new byte[0];
        String hash = Md5Crypt.apr1Crypt(empty, rnd);
        assertNotNull(hash);
        assertTrue(APR1_STRICT_PATTERN.matcher(hash).matches(), () -> "Hash does not match strict APR1 format: " + hash);
    }

    /**
     * Typical non‑empty password with a fixed seed (42) must yield a hash that
     * conforms to the strict APR‑1 format.
     */
    @Test
    void testApr1CryptTypicalPasswordDeterministicFormat() {
        Random rnd = new Random(42L);
        byte[] pwd = "password".getBytes(StandardCharsets.UTF_8);
        String hash = Md5Crypt.apr1Crypt(pwd, rnd);
        assertNotNull(hash);
        assertTrue(APR1_STRICT_PATTERN.matcher(hash).matches(), () -> "Hash does not match strict APR1 format: " + hash);
    }

    /**
     * Using the same {@link Random} instance for two consecutive calls must
     * generate two different salts (the Random's internal state advances).
     */
    @Test
    void testApr1CryptSuccessiveCallsGenerateDifferentSalts() {
        Random shared = new Random(12345L);
        byte[] pwd = "samePassword".getBytes(StandardCharsets.UTF_8);
        String first = Md5Crypt.apr1Crypt(pwd, shared);
        String second = Md5Crypt.apr1Crypt(pwd, shared);
        String salt1 = extractSalt(first);
        String salt2 = extractSalt(second);
        assertNotEquals(salt1, salt2, () -> String.format("Salts should differ but both were '%s'", salt1));
    }

    /**
     * Password containing non‑ASCII (including negative) bytes must still
     * produce a valid APR‑1 hash that matches the strict format.
     */
    @Test
    void testApr1CryptNonAsciiBytesFormat() {
        Random rnd = new Random(0L);
        byte[] nonAscii = new byte[] { (byte) 0xFF, (byte) 0x00, (byte) 0x7F };
        String hash = Md5Crypt.apr1Crypt(nonAscii, rnd);
        assertNotNull(hash);
        assertTrue(APR1_STRICT_PATTERN.matcher(hash).matches(), () -> "Hash for non‑ASCII bytes does not match strict APR1 format: " + hash);
    }

    @ParameterizedTest
    @MethodSource("formatTestProvider")
    void testApr1CryptVariousInputsMatchStrictPattern(FormatTestCase tc) {
        String hash = Md5Crypt.apr1Crypt(tc.keyBytes, tc.random);
        assertNotNull(hash);
        assertTrue(APR1_STRICT_PATTERN.matcher(hash).matches(), () -> "Hash does not match strict APR1 format: " + hash);
    }
}

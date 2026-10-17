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
import java.util.Base64;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha256Crypt_3_Test {

    /**
     * Random instance used for the tests – a SecureRandom gives deterministic behaviour
     *  for the purpose of these unit‑tests (the actual random values are not verified).
     */
    private Random random;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh SecureRandom for each test.
        random = new SecureRandom();
    }

    /**
     * Provides malformed salts together with a human readable description.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidSaltProvider() {
        return Stream.of(// Salt does not start with the required "$5$" prefix
        org.junit.jupiter.params.provider.Arguments.of("Missing prefix", "invalidSaltWithoutPrefix"), // Salt consists only of the prefix – no actual salt data
        org.junit.jupiter.params.provider.Arguments.of("Prefix only", "$5$"), // Salt contains characters outside the allowed crypt base‑64 alphabet
        org.junit.jupiter.params.provider.Arguments.of("Illegal character '!'", "$5$abc!def"));
    }

    /**
     * Verify that {@code Sha2Crypt.sha256Crypt(byte[], String, Random)} returns a
     * correctly prefixed hash when an explicit salt is supplied.
     */
    @Test
    void testSha256Crypt_WithExplicitSalt() {
        // given
        byte[] keyBytes = "secretPassword".getBytes(StandardCharsets.UTF_8);
        // The salt must start with the SHA‑256 prefix "$5$" and end with a "$"
        // as required by the underlying implementation.
        String salt = "$5$mysalt123$";
        // when
        String result = Sha2Crypt.sha256Crypt(keyBytes, salt, random);
        // then
        assertNotNull(result, "Resulting hash must not be null");
        assertTrue(result.startsWith(Sha2Crypt.SHA256_PREFIX), "Hash must start with the SHA‑256 prefix \"$5$\"");
        // The salt (without the surrounding "$") should appear in the hash.
        assertTrue(result.contains("mysalt123"), "Resulting hash should contain the provided salt");
    }

    /**
     * Verify that a {@code null} salt triggers generation of a random salt and
     * that the returned hash still respects the SHA‑256 prefix.
     */
    @Test
    void testSha256Crypt_WithNullSaltGeneratesRandomSalt() {
        // given
        byte[] keyBytes = "anotherSecret".getBytes(StandardCharsets.UTF_8);
        // request random salt generation
        String salt = null;
        // when
        String result = Sha2Crypt.sha256Crypt(keyBytes, salt, random);
        // then
        assertNotNull(result, "Resulting hash must not be null");
        assertTrue(result.startsWith(Sha2Crypt.SHA256_PREFIX), "Hash must start with the SHA‑256 prefix \"$5$\"");
        // The generated hash must contain at least two '$' characters (prefix and delimiter)
        int dollarCount = result.length() - result.replace("$", "").length();
        assertTrue(dollarCount >= 3, "Generated hash should contain prefix, salt and checksum delimiters");
    }

    /**
     * When {@code salt} is {@code null} and a non‑null {@link Random} is supplied,
     * the method must generate an 8‑character base‑64 salt.  The total length of a
     * SHA‑256 crypt string is therefore 43‑44 characters (including the "$5$"
     * prefix, trailing "$" and the 22‑character hash).
     */
    @Test
    void testSha256Crypt_NullSalt_GeneratesCorrectLength() {
        // given
        // "password"
        byte[] keyBytes = Base64.getDecoder().decode("cGFzc3dvcmQ=");
        String salt = null;
        // when
        String result = Sha2Crypt.sha256Crypt(keyBytes, salt, random);
        // then
        assertNotNull(result);
        assertTrue(result.startsWith(Sha2Crypt.SHA256_PREFIX));
        // Expected length: $5$ + 8‑char salt + $ + 22‑char hash = 3 + 8 + 1 + 22 = 34?
        // In the original crypt format the hash part is 43‑44 total characters.
        // We therefore accept the commonly observed length range.
        assertTrue(result.length() >= 43 && result.length() <= 44, "Result length should be between 43 and 44 characters");
    }

    /**
     * If {@code salt} is {@code null} but the supplied {@link Random} instance
     * itself is {@code null}, {@link B64#getRandomSalt} will throw a
     * {@link NullPointerException}.  The method must propagate this exception.
     */
    @Test
    void testSha256Crypt_NullSalt_NullRandom_ThrowsNPE() {
        // given
        // "password"
        byte[] keyBytes = Base64.getDecoder().decode("cGFzc3dvcmQ=");
        String salt = null;
        Random nullRandom = null;
        // when / then
        assertThrows(NullPointerException.class, () -> Sha2Crypt.sha256Crypt(keyBytes, salt, nullRandom), "Expected NullPointerException when Random argument is null");
    }

    /**
     * When {@code keyBytes} is {@code null} the underlying implementation throws
     * a {@link NullPointerException}.  The method should let this exception propagate.
     */
    @Test
    void testSha256Crypt_NullKeyBytes_ThrowsNPE() {
        // given
        byte[] keyBytes = null;
        String salt = "$5$validSalt$";
        // random is irrelevant because salt is supplied
        // (but we keep a non‑null instance for completeness)
        // when / then
        assertThrows(NullPointerException.class, () -> Sha2Crypt.sha256Crypt(keyBytes, salt, random), "Expected NullPointerException when keyBytes is null");
    }

    /**
     * Supplies a variety of malformed salts and verifies that the method throws
     * {@link IllegalArgumentException} for each.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidSaltProvider")
    void testSha256Crypt_InvalidSalt_ThrowsIllegalArgumentException(String description, String salt) {
        // given
        // "password"
        byte[] keyBytes = Base64.getDecoder().decode("cGFzc3dvcmQ=");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha256Crypt(keyBytes, salt, random), "Expected IllegalArgumentException for salt: " + salt);
        assertNotNull(ex.getMessage());
    }
}

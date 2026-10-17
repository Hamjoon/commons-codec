package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_apr1Crypt_Test {

    // $apr1$<salt (0‑8 chars from [A‑Za‑z0‑9./])>$<hash>
    private static final Pattern APR1_PATTERN = Pattern.compile("^\\$apr1\\$[A-Za-z0-9./]{0,8}\\$.+$");

    // $apr1$<salt (8 chars from [A‑Za‑z0‑9./])>$<hash (22 chars from same set)>
    private static final java.util.regex.Pattern APR1_REGEX = java.util.regex.Pattern.compile("^\\$apr1\\$[A-Za-z0-9./]{8}\\$[A-Za-z0-9./]{22}$");

    /**
     * Supplies the byte‑array test cases.
     *
     * @return stream of arguments: description and the corresponding byte array.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideApr1CryptByteArrayCases() {
        return Stream.of(// Typical ASCII password bytes: "password"
        org.junit.jupiter.params.provider.Arguments.of("Typical ASCII password bytes", // "password"
        new byte[] { 112, 97, 115, 115, 119, 111, 114, 100 }), // Empty byte array
        org.junit.jupiter.params.provider.Arguments.of("Empty byte array", new byte[] {}), // Byte array with null byte, high‑order bytes and non‑printable characters
        org.junit.jupiter.params.provider.Arguments.of("Mixed non‑printable bytes", new byte[] { 0, (byte) 0xFF, 16, 127, (byte) 0x80, (byte) 0xC8 }));
    }

    /**
     * Verify that the method returns a string that conforms to the APR1 format.
     */
    @Test
    void testApr1CryptProducesValidFormat() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        String result = Md5Crypt.apr1Crypt(key);
        assertNotNull(result, "Result must not be null");
        assertTrue(APR1_PATTERN.matcher(result).matches(), "Result does not match APR1 pattern: " + result);
    }

    /**
     * Passing {@code null} as the password must result in a {@link NullPointerException}.
     */
    @Test
    void testApr1CryptNullInputThrowsNPE() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.apr1Crypt((byte[]) null));
    }

    /**
     * The implementation is expected to zero‑out the supplied password array after
     * the hash is computed (same behaviour as {@link Md5Crypt#md5Crypt(byte[])}).
     */
    @Test
    void testApr1CryptZeroesInput() {
        byte[] password = new byte[64];
        // Fill the array with a non‑zero value so we can detect zeroing.
        for (int i = 0; i < password.length; i++) {
            password[i] = (byte) 0x5A;
        }
        // Perform the hash.
        Md5Crypt.apr1Crypt(password);
        // After the call the password array must be cleared.
        for (byte b : password) {
            assertEquals(0, b, "Password array should be zeroed after apr1Crypt");
        }
    }

    /**
     * When a deterministic salt is supplied via the overloaded method, the output
     * must be reproducible. This test uses the overload to generate an expected
     * value and then checks that the random‑salt variant still produces a valid
     * APR1 hash (the salt part will differ, but the format stays the same).
     */
    @Test
    void testApr1CryptWithExplicitSaltIsDeterministic() {
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        // 6‑character salt (no trailing $)
        String salt = "$apr1$mysalt";
        String expected = Md5Crypt.apr1Crypt(key, salt);
        // The expected value must match the APR1 pattern.
        assertTrue(APR1_PATTERN.matcher(expected).matches(), "Deterministic hash does not match APR1 pattern");
        // For comparison, call the random‑salt version and ensure it also matches the pattern.
        String randomResult = Md5Crypt.apr1Crypt(key);
        assertTrue(APR1_PATTERN.matcher(randomResult).matches(), "Random‑salt hash does not match APR1 pattern");
        // The two results must differ because the salts are different.
        assertNotEquals(expected, randomResult, "Hashes with different salts should not be equal");
    }

    /**
     * Passing {@code null} as the password must result in an {@link IllegalArgumentException}.
     */
    @Test
    void testApr1CryptNullInputThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.apr1Crypt((byte[]) null), "Null password should raise IllegalArgumentException");
    }

    /**
     * Parameterised test that verifies a variety of byte‑array inputs all
     * produce a syntactically correct APR1‑MD5 hash (8‑char salt, 22‑char hash).
     *
     * The test data mirrors the JSON group {@code apr1Crypt_byteArray}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideApr1CryptByteArrayCases")
    void testApr1CryptByteArrayValidFormats(String description, byte[] input) {
        // Act
        // clone to keep the original for later assertions
        String hash = Md5Crypt.apr1Crypt(input.clone());
        // Assert format
        assertNotNull(hash, "Hash must not be null");
        assertTrue(APR1_REGEX.matcher(hash).matches(), () -> "Hash does not match expected APR1 format for case: " + description + " -> " + hash);
    }

    /**
     * Verifies that passing {@code null} as the password array throws the
     * expected {@link IllegalArgumentException}.  This corresponds to the
     * JSON group {@code apr1Crypt_byteArray_exceptions}.
     */
    @Test
    void testApr1CryptByteArrayNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.apr1Crypt((byte[]) null), "Null password array must raise IllegalArgumentException");
    }
}

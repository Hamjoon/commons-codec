package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha512Crypt_Test{

    // Regular expression that matches the typical output of SHA‑512 crypt:
    // $6$<salt>$<hash>
    private static final Pattern SHA512_CRYPT_PATTERN = Pattern.compile("^\\$6\\$[^$]{1,16}\\$[A-Za-z0-9./]{86}$");

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that {@code sha512Crypt(byte[])} returns a non‑null string that
     * conforms to the expected SHA‑512‑crypt format when no explicit salt is supplied.
     */
    @Test
    void sha512Crypt_withoutSalt_returnsValidHash() {
        // given
        byte[] password = "secretPassword".getBytes(StandardCharsets.UTF_8);

        // when
        String result = Sha2Crypt.sha512Crypt(password);

        // then
        assertNotNull(result, "Result must not be null");
        assertTrue(SHA512_CRYPT_PATTERN.matcher(result).matches(),
                "Result should match the SHA‑512‑crypt pattern, but was: " + result);
    }

    /**
     * Verify that {@code sha512Crypt(byte[], String)} produces a hash that
     * contains the supplied salt.
     */
    @Test
    void sha512Crypt_withExplicitSalt_returnsHashContainingSalt() {
        // given
        byte[] password = "anotherSecret".getBytes(StandardCharsets.UTF_8);
        String salt = "mySalt123";

        // when
        String result = Sha2Crypt.sha512Crypt(password, salt);

        // then
        assertNotNull(result);
        // The result must start with "$6$mySalt123$"
        String expectedPrefix = "$6$" + salt + "$";
        assertTrue(result.startsWith(expectedPrefix),
                "Result should start with the provided salt prefix, but was: " + result);
        // And still match the overall pattern (hash length may vary with different salts)
        assertTrue(SHA512_CRYPT_PATTERN.matcher(result).matches(),
                "Result should match the SHA‑512‑crypt pattern, but was: " + result);
    }

    /**
     * Verify that a {@code null} key array leads to a {@link NullPointerException}.
     * The implementation does not guard against a null key, so the contract
     * is that an NPE is thrown.
     */
    @Test
    void sha512Crypt_nullKey_throwsNullPointerException() {
        // given
        byte[] nullKey = null;

        // when / then
        assertThrows(NullPointerException.class, () -> Sha2Crypt.sha512Crypt(nullKey));
    }
}
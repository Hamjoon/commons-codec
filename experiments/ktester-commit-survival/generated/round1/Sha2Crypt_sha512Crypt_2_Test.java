package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

/**
 * Tests for {@link Sha2Crypt#sha512Crypt(byte[], String)}.
 *
 * <p>
 * The tests verify that:
 * <ul>
 *   <li>When the salt argument is {@code null}, a valid SHA‑512 crypt string is generated
 *       and it starts with the required {@code "$6$"} prefix.</li>
 *   <li>When a valid salt (including the {@code "$6$"} prefix) is supplied, the resulting
 *       string contains that salt and also starts with the same prefix.</li>
 * </ul>
 * </p>
 *
 * No private members of {@link Sha2Crypt} are accessed; the class Sha2Crypt_sha512Crypt_2_Test* through its public API.
 */
@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha512Crypt_2_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
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
     * Verify that a call with {@code null} salt generates a result that starts with the
     * SHA‑512 prefix {@code "$6$"} and that the result is not {@code null} or empty.
     */
    @Test
    void sha512Crypt_WithNullSalt_GeneratesValidHash() {
        // given
        final byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        final String salt = null;

        // when
        final String result = Sha2Crypt.sha512Crypt(key, salt);

        // then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX),
                () -> "Result should start with SHA‑512 prefix \"" + Sha2Crypt.SHA512_PREFIX + "\" but was: " + result);
    }

    /**
     * Verify that providing an explicit, correctly‑prefixed salt yields a result that
     * contains the same salt (followed by a '$') and also starts with the SHA‑512 prefix.
     */
    @Test
    void sha512Crypt_WithExplicitSalt_UsesProvidedSalt() {
        // given
        final byte[] key = "anotherPassword".getBytes(StandardCharsets.UTF_8);
        final String customSalt = "mysalt123";
        final String salt = Sha2Crypt.SHA512_PREFIX + customSalt; // e.g. "$6$mysalt123"

        // when
        final String result = Sha2Crypt.sha512Crypt(key, salt);

        // then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.startsWith(salt + "$"),
                () -> "Result should start with the supplied salt followed by '$' but was: " + result);
        assertTrue(result.startsWith(Sha2Crypt.SHA512_PREFIX),
                () -> "Result should start with SHA‑512 prefix \"" + Sha2Crypt.SHA512_PREFIX + "\"");
    }
}
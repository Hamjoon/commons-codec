package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Sha2Crypt#sha256Crypt(byte[], String)}.
 *
 * <p>
 * The tests verify that:
 * <ul>
 *   <li>When {@code null} is passed as the salt a result is generated that starts with the SHA‑256
 *   prefix {@code "$5$"} and is not {@code null}.</li>
 *   <li>When a valid salt (including the prefix) is supplied the returned hash:
 *       <ul>
 *         <li>starts with the provided salt (including the prefix),</li>
 *         <li>is deterministic – the same input yields the same output.</li>
 *       </ul>
 *   </li>
 * </ul>
 * The implementation under test is pure static; no external resources or stateful
 * dependencies need to be mocked.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha256Crypt_2_Test{

    /** Sample password used in the deterministic test. */
    private static final byte[] SAMPLE_PASSWORD = "password".getBytes(StandardCharsets.UTF_8);

    /** A deterministic salt that already contains the required SHA‑256 prefix. */
    private static final String DETERMINISTIC_SALT = Sha2Crypt.SHA256_PREFIX + "testsalt";

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
     * Verifies that calling {@code sha256Crypt} with a {@code null} salt generates a
     * non‑null hash that begins with the SHA‑256 prefix.
     */
    @Test
    void testSha256Crypt_WithNullSalt_GeneratesHashWithPrefix() {
        // given
        byte[] key = "anyPassword".getBytes(StandardCharsets.UTF_8);
        // when
        String result = Sha2Crypt.sha256Crypt(key, null);
        // then
        assertNotNull(result, "Resulting hash must not be null");
        assertTrue(result.startsWith(Sha2Crypt.SHA256_PREFIX),
                () -> "Hash should start with the SHA‑256 prefix \"" + Sha2Crypt.SHA256_PREFIX + "\"");
        // The result should contain at least one additional '$' separator after the salt.
        assertTrue(result.indexOf('$', Sha2Crypt.SHA256_PREFIX.length()) > Sha2Crypt.SHA256_PREFIX.length(),
                "Result should contain a salt and hash part separated by '$'");
    }

    /**
     * Verifies that providing an explicit, deterministic salt (including the prefix)
     * yields a deterministic hash that starts with the supplied salt.
     */
    @Test
    void testSha256Crypt_WithExplicitSalt_IsDeterministic() {
        // given
        // SAMPLE_PASSWORD and DETERMINISTIC_SALT are defined as constants.
        // when
        String firstRun = Sha2Crypt.sha256Crypt(SAMPLE_PASSWORD, DETERMINISTIC_SALT);
        String secondRun = Sha2Crypt.sha256Crypt(SAMPLE_PASSWORD, DETERMINISTIC_SALT);
        // then
        assertNotNull(firstRun, "First run result must not be null");
        assertNotNull(secondRun, "Second run result must not be null");

        // Both runs must produce the identical hash.
        assertEquals(firstRun, secondRun, "Hash must be deterministic for the same input and salt");

        // The hash must start with the provided salt (including the prefix) followed by a '$'.
        String expectedPrefix = DETERMINISTIC_SALT + "$";
        assertTrue(firstRun.startsWith(expectedPrefix),
                () -> "Hash should start with the supplied salt \"" + expectedPrefix + "\"");
    }
}
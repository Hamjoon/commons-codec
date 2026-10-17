package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha512Crypt_Test {

    // -------------------------------------------------------------------------
    private static final Pattern SHA512_CRYPT_PATTERN = Pattern.compile("^\\$6\\$[A-Za-z0-9./]{8,16}\\$[A-Za-z0-9./]{86}$");

    /**
     * Provides a variety of password byte arrays that should result in a
     * correctly formatted SHA‑512‑crypt string when no explicit salt is given.
     */
    private static Stream<Arguments> provideKeyBytesForNoSalt() {
        return Stream.of(Arguments.of("password".getBytes(StandardCharsets.UTF_8), "regular ASCII password"), Arguments.of(new byte[0], "empty password"), Arguments.of(new byte[] { (byte) 0xFF, (byte) 0x00, (byte) 0x7A }, "non‑ASCII byte values"));
    }

    /**
     * Supplies a selection of malformed salts for the overload
     * {@code sha512Crypt(byte[], String)}.
     */
    private static Stream<Arguments> provideInvalidSalts() {
        return Stream.of(// Wrong prefix ($5$ is for SHA‑256)
        Arguments.of("$5$invalidSalt$", "Invalid prefix"), // Too short salt (only 2 chars between delimiters)
        Arguments.of("$6$ab$", "Salt too short"), // Too long salt (17 characters, exceeds max 16)
        Arguments.of("$6$abcdefghijklmnopq$", "Salt too long"));
    }

    @Test
    void sha512Crypt_withNonNullKey_returnsValidHash() {
        // given
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        // when
        String hash = Sha2Crypt.sha512Crypt(key);
        // then
        assertNotNull(hash, "Hash must not be null");
        assertTrue(hash.startsWith(Sha2Crypt.SHA512_PREFIX), "Hash must start with the SHA‑512 prefix \"$6$\"");
        // Basic sanity check – a SHA‑512 crypt string contains at least 2 '$' delimiters
        assertEquals(3, hash.chars().filter(ch -> ch == '$').count(), "Hash should contain exactly three '$' characters");
    }

    @Test
    void sha512Crypt_withEmptyKey_returnsValidHash() {
        // given
        byte[] emptyKey = new byte[0];
        // when
        String hash = Sha2Crypt.sha512Crypt(emptyKey);
        // then
        assertNotNull(hash);
        assertTrue(hash.startsWith(Sha2Crypt.SHA512_PREFIX));
    }

    @Test
    void sha512Crypt_sameKeyDifferentCallsProduceDifferentSalts() {
        // given
        byte[] key = "samePassword".getBytes(StandardCharsets.UTF_8);
        // when
        String hash1 = Sha2Crypt.sha512Crypt(key);
        String hash2 = Sha2Crypt.sha512Crypt(key);
        // then
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2, "Two invocations without an explicit salt should generate different hashes because a random salt is used each time");
    }

    @Test
    void sha512Crypt_withExplicitSalt_isDeterministic() {
        // given
        byte[] key = "deterministic".getBytes(StandardCharsets.UTF_8);
        // a valid SHA‑512 salt format
        String explicitSalt = "$6$rounds=5000$abcdefghijklmnop$";
        // when
        String hash1 = Sha2Crypt.sha512Crypt(key, explicitSalt);
        String hash2 = Sha2Crypt.sha512Crypt(key, explicitSalt);
        // then
        assertEquals(hash1, hash2, "Using the same explicit salt must yield identical hashes");
        assertTrue(hash1.startsWith(Sha2Crypt.SHA512_PREFIX));
    }

    @ParameterizedTest(name = "[{index}] sha512Crypt_noSalt – {1}")
    @MethodSource("provideKeyBytesForNoSalt")
    void sha512Crypt_noSalt_validFormat(byte[] keyBytes, String description) {
        // when
        String hash = Sha2Crypt.sha512Crypt(keyBytes);
        // then
        assertNotNull(hash, "Hash must not be null");
        assertTrue(hash.startsWith(Sha2Crypt.SHA512_PREFIX), "Hash must start with the SHA‑512 prefix \"$6$\"");
        assertTrue(SHA512_CRYPT_PATTERN.matcher(hash).matches(), "Hash must match the expected crypt format: " + hash);
    }

    /**
     * Verifies that two successive invocations with the same key generate
     * different salts and therefore different hash strings.
     */
    @Test
    void sha512Crypt_noSalt_randomSaltProducesDifferentHashes() {
        // given
        byte[] key = "password".getBytes(StandardCharsets.UTF_8);
        // when
        String hash1 = Sha2Crypt.sha512Crypt(key);
        String hash2 = Sha2Crypt.sha512Crypt(key);
        // then
        assertNotEquals(hash1, hash2, "Two calls without an explicit salt must produce different hashes");
    }

    @Test
    void sha512Crypt_withExplicitValidSalt_isDeterministic() {
        // given
        byte[] key = "deterministic".getBytes(StandardCharsets.UTF_8);
        // A valid salt: prefix $6$, optional rounds spec, then 8‑16 chars, ending with $
        String validSalt = "$6$rounds=5000$abcdefghijklmnop$";
        // when
        String hash1 = Sha2Crypt.sha512Crypt(key, validSalt);
        String hash2 = Sha2Crypt.sha512Crypt(key, validSalt);
        // then
        assertEquals(hash1, hash2, "Using the same explicit salt must yield identical hashes");
        assertTrue(hash1.startsWith(Sha2Crypt.SHA512_PREFIX));
        assertTrue(SHA512_CRYPT_PATTERN.matcher(hash1).matches(), "Deterministic hash must also match the expected format");
    }

    @Test
    void sha512Crypt_nullKeyBytes_throwsNullPointerException() {
        // given
        byte[] key = null;
        // when / then
        assertThrows(NullPointerException.class, () -> Sha2Crypt.sha512Crypt(key), "Passing null as keyBytes should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] sha512Crypt_invalidSalt – {1}")
    @MethodSource("provideInvalidSalts")
    void sha512Crypt_invalidSalt_throwsIllegalArgumentException(byte[] dummyKey, String description) {
        // The key itself is irrelevant for the validation of the salt format.
        byte[] key = "test".getBytes(StandardCharsets.UTF_8);
        // dummyKey actually holds the salt string due to type cast trick
        String malformedSalt = (String) dummyKey;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(key, malformedSalt), "Providing a malformed salt should raise IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("invalid salt"), "Exception message should contain \"Invalid salt\": " + ex.getMessage());
    }

    @Test
    void sha512Crypt_saltTooShort_throwsIllegalArgumentException() {
        byte[] key = "test".getBytes(StandardCharsets.UTF_8);
        // only 2 characters
        String shortSalt = "$6$ab$";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(key, shortSalt));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid salt"), "Message should complain about invalid salt length");
    }

    @Test
    void sha512Crypt_saltTooLong_throwsIllegalArgumentException() {
        byte[] key = "test".getBytes(StandardCharsets.UTF_8);
        // 17 characters
        String longSalt = "$6$abcdefghijklmnopq$";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Sha2Crypt.sha512Crypt(key, longSalt));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid salt"), "Message should complain about invalid salt length");
    }
}

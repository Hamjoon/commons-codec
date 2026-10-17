package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_apr1Crypt_5_Test{

    /** Sample key used by the tests – initialised before each test. */
    private byte[] sampleKey;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a deterministic key to avoid any randomness in the test logic.
        sampleKey = "password".getBytes(StandardCharsets.UTF_8);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference – not strictly required but keeps the test tidy.
        sampleKey = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * When the supplied salt is {@code null} the method must forward {@code null}
     * to {@link Md5Crypt#md5Crypt(byte[], String, String)} without adding the
     * APR1 prefix.
     */
    @Test
    void apr1Crypt_nullSalt_forwardsNull() {
        try (MockedStatic<Md5Crypt> mocked = mockStatic(Md5Crypt.class)) {
            // Arrange – stub the static call we expect.
            mocked.when(() -> Md5Crypt.md5Crypt(eq(sampleKey), isNull(), eq(Md5Crypt.APR1_PREFIX)))
                  .thenReturn("hashed-null-salt");

            // Act
            String result = Md5Crypt.apr1Crypt(sampleKey, null);

            // Assert
            assertEquals("hashed-null-salt", result);
            // Verify that the static method was called exactly once with the expected arguments.
            mocked.verify(() -> Md5Crypt.md5Crypt(eq(sampleKey), isNull(), eq(Md5Crypt.APR1_PREFIX)), times(1));
        }
    }

    /**
     * When the supplied salt does not start with the APR1 prefix the method must
     * prepend the prefix before delegating to {@code md5Crypt}.
     */
    @Test
    void apr1Crypt_saltWithoutPrefix_prependsPrefix() {
        String rawSalt = "mysalt";
        String expectedSalt = Md5Crypt.APR1_PREFIX + rawSalt; // "$apr1$mysalt"

        try (MockedStatic<Md5Crypt> mocked = mockStatic(Md5Crypt.class)) {
            mocked.when(() -> Md5Crypt.md5Crypt(eq(sampleKey), eq(expectedSalt), eq(Md5Crypt.APR1_PREFIX)))
                  .thenReturn("hashed-no-prefix");

            String result = Md5Crypt.apr1Crypt(sampleKey, rawSalt);

            assertEquals("hashed-no-prefix", result);
            mocked.verify(() -> Md5Crypt.md5Crypt(eq(sampleKey), eq(expectedSalt), eq(Md5Crypt.APR1_PREFIX)), times(1));
        }
    }

    /**
     * When the supplied salt already contains the APR1 prefix the method must
     * leave it untouched.
     */
    @Test
    void apr1Crypt_saltWithPrefix_keepsOriginal() {
        String prefixedSalt = Md5Crypt.APR1_PREFIX + "alreadyPrefixed";

        try (MockedStatic<Md5Crypt> mocked = mockStatic(Md5Crypt.class)) {
            mocked.when(() -> Md5Crypt.md5Crypt(eq(sampleKey), eq(prefixedSalt), eq(Md5Crypt.APR1_PREFIX)))
                  .thenReturn("hashed-with-prefix");

            String result = Md5Crypt.apr1Crypt(sampleKey, prefixedSalt);

            assertEquals("hashed-with-prefix", result);
            mocked.verify(() -> Md5Crypt.md5Crypt(eq(sampleKey), eq(prefixedSalt), eq(Md5Crypt.APR1_PREFIX)), times(1));
        }
    }

    /**
     * Verify that an empty password array is still processed and delegated correctly.
     */
    @Test
    void apr1Crypt_emptyKeyArray_delegatesCorrectly() {
        byte[] emptyKey = new byte[0];
        String salt = "any";

        try (MockedStatic<Md5Crypt> mocked = mockStatic(Md5Crypt.class)) {
            String expectedSalt = Md5Crypt.APR1_PREFIX + salt;
            mocked.when(() -> Md5Crypt.md5Crypt(eq(emptyKey), eq(expectedSalt), eq(Md5Crypt.APR1_PREFIX)))
                  .thenReturn("hashed-empty-key");

            String result = Md5Crypt.apr1Crypt(emptyKey, salt);

            assertEquals("hashed-empty-key", result);
            mocked.verify(() -> Md5Crypt.md5Crypt(eq(emptyKey), eq(expectedSalt), eq(Md5Crypt.APR1_PREFIX)), times(1));
        }
    }
}
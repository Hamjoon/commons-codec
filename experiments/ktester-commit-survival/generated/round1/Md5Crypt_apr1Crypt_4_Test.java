package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_apr1Crypt_4_Test{

    /** Sample password used by several tests */
    private static final byte[] SAMPLE_KEY = "password".getBytes(StandardCharsets.UTF_8);
    /** A deterministic Random – it will never be consulted because we mock B64 */
    private static final Random DUMMY_RANDOM = new Random(0L);
    /** The salt that the mocked B64.getRandomSalt will return */
    private static final String MOCKED_SALT = "abcd1234";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verifies that {@code apr1Crypt(byte[], Random)} builds the result by delegating to
     * {@code apr1Crypt(byte[], String)} with a salt that consists of the APR1 prefix
     * followed by the value returned from {@code B64.getRandomSalt}.
     */
    @Test
    void testApr1CryptUsesMockedSalt() {
        // Mock the static B64.getRandomSalt method so we can control the generated salt.
        try (MockedStatic<B64> b64Mock = mockStatic(B64.class)) {
            b64Mock.when(() -> B64.getRandomSalt(8, DUMMY_RANDOM)).thenReturn(MOCKED_SALT);

            // Call the method under test.
            String actual = Md5Crypt.apr1Crypt(SAMPLE_KEY, DUMMY_RANDOM);

            // Compute the expected value by calling the overload that accepts an explicit salt.
            String expectedSalt = Md5Crypt.APR1_PREFIX + MOCKED_SALT;
            String expected = Md5Crypt.apr1Crypt(SAMPLE_KEY, expectedSalt);

            assertEquals(expected, actual,
                    "apr1Crypt(byte[], Random) should produce the same result as apr1Crypt(byte[], String) "
                    + "when the random salt is mocked.");
        }
    }

    /**
     * The method must reject a {@code null} password array.
     */
    @Test
    void testApr1CryptNullKeyThrowsNpe() {
        assertThrows(NullPointerException.class,
                () -> Md5Crypt.apr1Crypt((byte[]) null, DUMMY_RANDOM),
                "Calling apr1Crypt with a null key should throw NullPointerException");
    }

    /**
     * Passing a {@code null} {@link Random} leads to a {@link NullPointerException}
     * because the static utility tries to use it.
     */
    @Test
    void testApr1CryptNullRandomThrowsNpe() {
        assertThrows(NullPointerException.class,
                () -> Md5Crypt.apr1Crypt(SAMPLE_KEY, null),
                "Calling apr1Crypt with a null Random should throw NullPointerException");
    }

    /**
     * Basic sanity check: when a real {@link Random} is used, the result still starts
     * with the APR1 prefix and matches the expected pattern.
     */
    @Test
    void testApr1CryptRealRandomProducesValidFormat() {
        Random realRandom = new java.security.SecureRandom();
        String result = Md5Crypt.apr1Crypt(SAMPLE_KEY, realRandom);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.startsWith(Md5Crypt.APR1_PREFIX),
                "Result must start with the APR1 prefix");
        // The format is: $apr1$<8‑char‑salt>$<hash>
        assertTrue(result.matches("^\\$apr1\\$[A-Za-z0-9./]{8}\\$.+$"),
                "Result must match the APR1 crypt format");
    }
}
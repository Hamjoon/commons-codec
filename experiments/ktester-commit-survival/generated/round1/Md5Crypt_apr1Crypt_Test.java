package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class) // kept from the template; not used directly
class Md5Crypt_apr1Crypt_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
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
     * Verify that the one‑argument {@code apr1Crypt(byte[])} method returns a
     * string that follows the APR1 format and that a random salt of up to
     * eight characters is generated.
     */
    @Test
    void testApr1CryptByteArrayPattern() {
        byte[] data = "password".getBytes(StandardCharsets.UTF_8);
        String result = Md5Crypt.apr1Crypt(data);

        // Expected format: $apr1$<salt>$<hash>
        assertNotNull(result);
        assertTrue(result.matches("^\\$apr1\\$[A-Za-z0-9./]{1,8}\\$.+$"),
                "Result does not match APR1 pattern: " + result);

        // Extract the salt and verify its length (1‑8 characters)
        Matcher m = Pattern.compile("^\\$apr1\\$([A-Za-z0-9./]{1,8})\\$(.+)$")
                           .matcher(result);
        assertTrue(m.find(), "Unable to extract salt from result");
        String salt = m.group(1);
        assertTrue(salt.length() >= 1 && salt.length() <= 8,
                "Generated salt length out of bounds: " + salt.length());
    }

    /**
     * The overload that accepts an explicit salt must honour that salt.
     * This test ensures that the provided salt is reflected verbatim in the
     * output.
     */
    @Test
    void testApr1CryptWithExplicitSalt() {
        byte[] data = "secret".getBytes(StandardCharsets.UTF_8);
        String explicitSalt = "$apr1$mysalt";
        String result = Md5Crypt.apr1Crypt(data, explicitSalt);

        assertNotNull(result);
        // The result must start with the exact salt we supplied (including the prefix)
        assertTrue(result.startsWith(explicitSalt + "$"),
                "Result does not start with the explicit salt: " + result);
    }

    /**
     * An empty byte array should still produce a valid APR1 hash.
     */
    @Test
    void testApr1CryptEmptyByteArray() {
        byte[] empty = new byte[0];
        String result = Md5Crypt.apr1Crypt(empty);

        assertNotNull(result);
        assertTrue(result.matches("^\\$apr1\\$[A-Za-z0-9./]{1,8}\\$.+$"),
                "Empty input did not produce a valid APR1 hash: " + result);
    }

    /**
     * Passing {@code null} as the key should raise a {@link NullPointerException}.
     */
    @Test
    void testApr1CryptNullInputThrows() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.apr1Crypt((byte[]) null));
    }
}
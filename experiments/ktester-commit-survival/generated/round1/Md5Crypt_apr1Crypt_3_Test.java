package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_apr1Crypt_3_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for apr1Crypt tests
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

    /** Verify that a normal call returns a string that follows the APR1 format. */
    @Test
    void testApr1CryptProducesValidHash() {
        // given
        final String password = "secret";
        final String salt = "abcd";

        // when
        final String result = Md5Crypt.apr1Crypt(password, salt);

        // then
        assertNotNull(result);
        // Expected format: $apr1$<salt up to 8 chars>$<hash>
        final Pattern pattern = Pattern.compile("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.+$");
        assertTrue(pattern.matcher(result).matches(),
                () -> "Result does not match APR1 pattern: " + result);
        // The salt part should be exactly the one we supplied (no truncation here)
        assertTrue(result.startsWith("$apr1$abcd$"),
                () -> "Result does not contain expected salt: " + result);
    }

    /** Salt longer than the maximum allowed length (8) must be truncated. */
    @Test
    void testApr1CryptSaltTruncation() {
        // given
        final String password = "secret";
        final String longSalt = "1234567890ABCDEF"; // >8 characters

        // when
        final String result = Md5Crypt.apr1Crypt(password, longSalt);

        // then
        assertNotNull(result);
        // The salt part should be the first 8 characters of the supplied salt
        final String expectedSalt = longSalt.substring(0, 8);
        assertTrue(result.startsWith("$apr1$" + expectedSalt + "$"),
                () -> "Salt was not truncated correctly. Expected prefix $" + expectedSalt + "$, got: " + result);
    }

    /** A dollar sign in the supplied salt terminates the salt early. */
    @Test
    void testApr1CryptSaltWithDollarSign() {
        // given
        final String password = "secret";
        final String saltWithDollar = "1234$567";

        // when
        final String result = Md5Crypt.apr1Crypt(password, saltWithDollar);

        // then
        assertNotNull(result);
        // Only the part before the first '$' should be used as the salt
        assertTrue(result.startsWith("$apr1$1234$"),
                () -> "Salt before dollar sign not used correctly. Result: " + result);
    }

    /** An empty salt is illegal and must cause an IllegalArgumentException. */
    @Test
    void testApr1CryptEmptySalt() {
        // given
        final String password = "secret";
        final String emptySalt = "";

        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> Md5Crypt.apr1Crypt(password, emptySalt),
                "Empty salt should throw IllegalArgumentException");
    }

    /** A null password must cause a NullPointerException. */
    @Test
    void testApr1CryptNullPassword() {
        // given
        final String nullPassword = null;
        final String salt = "abcd";

        // when / then
        assertThrows(NullPointerException.class,
                () -> Md5Crypt.apr1Crypt(nullPassword, salt),
                "Null password should throw NullPointerException");
    }

    /** A null salt should be accepted and a random salt generated internally. */
    @Test
    void testApr1CryptNullSaltGeneratesRandomSalt() {
        // given
        final String password = "secret";
        final String nullSalt = null;

        // when
        final String result = Md5Crypt.apr1Crypt(password, nullSalt);

        // then
        assertNotNull(result);
        // Verify the result still matches the APR1 pattern
        final Pattern pattern = Pattern.compile("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.+$");
        Matcher matcher = pattern.matcher(result);
        assertTrue(matcher.matches(),
                () -> "Result with null salt does not match APR1 pattern: " + result);
    }
}
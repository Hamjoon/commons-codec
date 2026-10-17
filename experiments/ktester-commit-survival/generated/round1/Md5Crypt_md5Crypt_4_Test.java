package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_md5Crypt_4_Test{

    /* --------------------------------------------------------------------- */
    /*  Lifecycle methods – nothing special is required for these tests.   */
    /* --------------------------------------------------------------------- */
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

    /* --------------------------------------------------------------------- */
    /*  Tests for Md5Crypt.md5Crypt(byte[], String)                           */
    /* --------------------------------------------------------------------- */

    @Test
    void md5Crypt_emptyByteArrayAndSimpleSalt() {
        // An empty password must produce the same hash as the reference implementation.
        String expected = "$1$foo$9mS5ExwgIECGE5YKlD5o91";
        String actual = Md5Crypt.md5Crypt(new byte[0], "$1$foo");
        assertEquals(expected, actual);
    }

    @Test
    void md5Crypt_saltTruncatedToEightChars() {
        // Salt longer than eight characters (after the prefix) must be truncated.
        String longSalt = "$1$1234567890123456"; // 16 chars after the prefix
        String expected = "$1$12345678$hj0uLpdidjPhbMMZeno8X/";
        String actual = Md5Crypt.md5Crypt("secret".getBytes(StandardCharsets.UTF_8), longSalt);
        assertEquals(expected, actual);
    }

    @Test
    void md5Crypt_saltCutAtFirstDollarSign() {
        // Anything after the first '$' following the prefix is ignored.
        String saltWithExtra = "$1$1234$567$890";
        String expected = "$1$1234$ImZYBLmYC.rbBKg9ERxX70";
        String actual = Md5Crypt.md5Crypt("secret".getBytes(StandardCharsets.UTF_8), saltWithExtra);
        assertEquals(expected, actual);
    }

    @Test
    void md5Crypt_nullKeyBytes_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> Md5Crypt.md5Crypt((byte[]) null, "$1$foo"));
    }

    @Test
    void md5Crypt_emptySalt_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Md5Crypt.md5Crypt("secret".getBytes(StandardCharsets.UTF_8), ""));
    }

    @Test
    void md5Crypt_zeroesOutInputArray() {
        // The method must overwrite the password bytes with zeros before returning.
        byte[] password = new byte[32];
        Arrays.fill(password, (byte) 'A');
        Md5Crypt.md5Crypt(password, "$1$foo");
        assertArrayEquals(new byte[password.length], password,
                "Password array should be cleared (filled with zeros) after hashing");
    }
}
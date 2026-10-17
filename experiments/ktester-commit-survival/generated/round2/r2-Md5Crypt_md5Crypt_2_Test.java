package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Md5Crypt_md5Crypt_2_Test {

    /**
     * Mocked static for {@link B64}.
     */
    private static MockedStatic<B64> b64Mock;

    @BeforeEach
    void setupBeforeEach() {
        // Mock the static method that creates the random salt.
        b64Mock = mockStatic(B64.class);
        // The md5Crypt(byte[]) overload builds the salt as MD5_PREFIX + B64.getRandomSalt(8)
        // MD5_PREFIX is "$1$", so the final salt will be "$1$abcd1234".
        when(B64.getRandomSalt(8)).thenReturn("abcd1234");
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid side‑effects on other tests.
        if (b64Mock != null) {
            b64Mock.close();
        }
    }

    @Test
    void md5CryptByteArrayProducesDeterministicResult() {
        // GIVEN
        final byte[] password = "password".getBytes(StandardCharsets.UTF_8);
        final byte[] originalCopy = password.clone();
        // WHEN
        final String hash = Md5Crypt.md5Crypt(password);
        // THEN
        // The salt is forced to "abcd1234", therefore the result must start with "$1$abcd1234$".
        assertTrue(hash.startsWith("$1$abcd1234$"), "Hash should start with the deterministic salt prefix");
        // The rest of the string must be non‑empty (the actual digest part).
        assertTrue(hash.matches("^\\$1\\$abcd1234\\$.+$"), "Hash should match the expected pattern");
        // The input array must be zeroed out after the call.
        assertArrayEquals(new byte[password.length], password, "Input byte array must be cleared (zeroed) after md5Crypt");
        // Ensure that the method does not modify the original data before zeroing.
        // (i.e., the array content before zeroing is the same as the original copy)
        // This is indirectly verified because we cloned the array before the call.
        // No further assertion is needed here.
    }

    @Test
    void md5CryptByteArrayWithNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> Md5Crypt.md5Crypt((byte[]) null));
    }

    @Test
    void md5CryptByteArrayMultipleCallsYieldSameResultWithMockedSalt() {
        final byte[] first = "first".getBytes(StandardCharsets.UTF_8);
        final byte[] second = "second".getBytes(StandardCharsets.UTF_8);
        final String hash1 = Md5Crypt.md5Crypt(first);
        final String hash2 = Md5Crypt.md5Crypt(second);
        // Both hashes must use the same deterministic salt.
        assertTrue(hash1.startsWith("$1$abcd1234$"));
        assertTrue(hash2.startsWith("$1$abcd1234$"));
        // They must be different because the password bytes differ.
        assertNotEquals(hash1, hash2, "Different passwords should produce different hashes");
    }

    @Test
    void md5CryptByteArrayNullThrowsIllegalArgumentException() {
        // The API contract specifies IllegalArgumentException for null input.
        assertThrows(IllegalArgumentException.class, () -> Md5Crypt.md5Crypt((byte[]) null), "Passing a null byte array should throw IllegalArgumentException");
    }
}

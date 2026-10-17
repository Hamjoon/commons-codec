package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base64_decodeInteger_Test{

    /** Random generator used for generating large integers */
    private static Random random;

    @BeforeAll
    static void setupBeforeAll() {
        random = new Random(0L); // deterministic seed for reproducible tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No instance state required for the static method under test.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verifies that a simple positive integer is correctly decoded.
     */
    @Test
    void testDecodeIntegerSimple() {
        final BigInteger original = BigInteger.valueOf(123456789L);
        final byte[] encoded = Base64.encodeInteger(original);
        final BigInteger decoded = Base64.decodeInteger(encoded);
        assertEquals(original, decoded, "Decoded integer should match the original value");
    }

    /**
     * Verifies that zero is correctly encoded and decoded.
     */
    @Test
    void testDecodeIntegerZero() {
        final BigInteger original = BigInteger.ZERO;
        final byte[] encoded = Base64.encodeInteger(original);
        final BigInteger decoded = Base64.decodeInteger(encoded);
        assertEquals(original, decoded, "Decoded zero should equal BigInteger.ZERO");
    }

    /**
     * Verifies that a large (multi‑byte) integer is correctly decoded.
     */
    @Test
    void testDecodeIntegerLargeNumber() {
        // Create a 256‑bit random positive integer.
        final byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        // Ensure the most‑significant bit is zero to keep the number positive.
        randomBytes[0] = (byte) (randomBytes[0] & 0x7F);
        final BigInteger original = new BigInteger(1, randomBytes);

        final byte[] encoded = Base64.encodeInteger(original);
        final BigInteger decoded = Base64.decodeInteger(encoded);

        assertEquals(original, decoded, "Decoded large integer should match the original value");
    }

    /**
     * Verifies that passing {@code null} results in a {@link NullPointerException}
     * because {@code decodeBase64(null)} returns {@code null} and the {@code BigInteger}
     * constructor does not accept a {@code null} byte array.
     */
    @Test
    void testDecodeIntegerNullInput() {
        assertThrows(NullPointerException.class, () -> Base64.decodeInteger(null),
                "decodeInteger(null) should throw NullPointerException");
    }

    /**
     * Verifies that malformed Base64 data (e.g., containing illegal characters)
     * leads to an {@link IllegalArgumentException} from the underlying decoder.
     */
    @Test
    void testDecodeIntegerInvalidBase64() {
        // Create a byte array that is not valid Base64 (contains '@' which is not in the alphabet).
        final byte[] invalidBase64 = new byte[] { '@', '@', '@', '@' };
        assertThrows(IllegalArgumentException.class, () -> Base64.decodeInteger(invalidBase64),
                "decodeInteger with invalid Base64 data should throw IllegalArgumentException");
    }
}
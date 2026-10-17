package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Base32#isInAlphabet(byte)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>valid characters for the default (RFC‑4648) alphabet</li>
 *   <li>valid characters for the Base32hex variant</li>
 *   <li>characters that are not part of the alphabet</li>
 *   <li>boundary conditions such as negative values and values outside the
 *       decode table range</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class Base32_isInAlphabet_Test{

    /** Instance using the standard Base32 alphabet. */
    private Base32 base32Standard;

    /** Instance using the Base32hex alphabet. */
    private Base32 base32Hex;

    /* ---------------------------------------------------------------------- */
    /*  Lifecycle methods                                                    */
    /* ---------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default alphabet (RFC‑4648)
        base32Standard = new Base32();

        // Hex alphabet (RFC‑4648 §2.2)
        base32Hex = new Base32(true);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per test.
        base32Standard = null;
        base32Hex = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for the default alphabet                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    void testStandardAlphabet_ValidCharacter() {
        // 'A' is the first character of the standard Base32 alphabet.
        byte octet = (byte) 'A';
        assertTrue(base32Standard.isInAlphabet(octet),
                "'A' should be recognised as part of the standard Base32 alphabet");
    }

    @Test
    void testStandardAlphabet_InvalidCharacter_LowerCase() {
        // Lower‑case letters are not part of the standard alphabet.
        byte octet = (byte) 'a';
        assertFalse(base32Standard.isInAlphabet(octet),
                "'a' should NOT be recognised as part of the standard Base32 alphabet");
    }

    @Test
    void testStandardAlphabet_InvalidCharacter_NotInTable() {
        // The character ':' is not defined in the Base32 alphabet.
        byte octet = (byte) ':';
        assertFalse(base32Standard.isInAlphabet(octet),
                "':' should NOT be recognised as part of the standard Base32 alphabet");
    }

    @Test
    void testStandardAlphabet_NegativeOctet() {
        // Negative values are rejected outright.
        byte octet = -1; // 0xFF as signed byte
        assertFalse(base32Standard.isInAlphabet(octet),
                "Negative octet should be rejected");
    }

    @Test
    void testStandardAlphabet_OctetBeyondDecodeTable() {
        // The decode table length is 128; 128 is out of range.
        int outOfRange = 128;
        // Cast to byte truncates to -128, which is negative, so we need an int version.
        // Use the overload that takes int via byte promotion.
        // Since the method signature expects a byte, we cannot pass 128 directly.
        // Instead we verify that any positive value >= decodeTable.length returns false.
        // The maximum positive byte value is 127, which is within range but maps to -1.
        byte octet = (byte) 127;
        assertFalse(base32Standard.isInAlphabet(octet),
                "Octet 127 is within range but not part of the alphabet, should return false");
    }

    @Test
    void testStandardAlphabet_PaddingCharacter() {
        // Padding character '=' is explicitly prohibited by the constructor,
        // but we can still verify that isInAlphabet returns false.
        byte octet = (byte) '=';
        assertFalse(base32Standard.isInAlphabet(octet),
                "Padding character '=' should not be considered part of the alphabet");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for the hex alphabet                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void testHexAlphabet_ValidCharacter_Zero() {
        // In the hex variant, '0' is the first character.
        byte octet = (byte) '0';
        assertTrue(base32Hex.isInAlphabet(octet),
                "'0' should be recognised as part of the Base32hex alphabet");
    }

    @Test
    void testHexAlphabet_ValidCharacter_Ten() {
        // 'A' corresponds to value 10 in the hex alphabet.
        byte octet = (byte) 'A';
        assertTrue(base32Hex.isInAlphabet(octet),
                "'A' should be recognised as part of the Base32hex alphabet");
    }

    @Test
    void testHexAlphabet_InvalidCharacter_LowerCase() {
        // Lower‑case letters are not part of the hex alphabet.
        byte octet = (byte) 'a';
        assertFalse(base32Hex.isInAlphabet(octet),
                "'a' should NOT be recognised as part of the Base32hex alphabet");
    }

    @Test
    void testHexAlphabet_InvalidCharacter_NotInTable() {
        // The character 'Z' is not part of the hex alphabet.
        byte octet = (byte) 'Z';
        assertFalse(base32Hex.isInAlphabet(octet),
                "'Z' should NOT be recognised as part of the Base32hex alphabet");
    }

    @Test
    void testHexAlphabet_NegativeOctet() {
        byte octet = -5;
        assertFalse(base32Hex.isInAlphabet(octet),
                "Negative octet should be rejected for hex alphabet as well");
    }
}
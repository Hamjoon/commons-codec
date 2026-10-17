package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

// Mockito not used but kept from template
@ExtendWith(MockitoExtension.class)
class Base64_isInAlphabet_Test {

    /**
     * Instance under test
     */
    private Base64 base64;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it builds the standard decode table.
        base64 = new Base64();
    }

    @AfterEach
    void teardownAfterEach() {
        base64 = null;
    }

    /**
     * Supplies the test data for {@link #testIsInAlphabet_various(int, boolean, String)}.
     *
     * The data mirrors the JSON test cases (group “Base64.isInAlphabet”).  The
     * decode table used by the default {@link Base64} instance is the standard
     * RFC‑4648 table, therefore the expected outcomes correspond to the official
     * Base64 alphabet.
     */
    private static Stream<Arguments> provideIsInAlphabetCases() {
        return Stream.of(// negative octet – first guard (octet >= 0) fails
        Arguments.of(-5, false, "Octet is negative; should short‑circuit to false"), // within range but entry is -1 (e.g., a digit not in alphabet)
        Arguments.of(10, false, "Octet 10 is within table but maps to -1, therefore false"), // octet >= decodeTable.length (standard length 128)
        Arguments.of(200, false, "Octet far beyond decode table length; should be false"), // valid alphabet character (index 3 corresponds to 'D')
        Arguments.of(3, true, "Octet 3 is part of the alphabet (maps to 'D')"), // NUL byte – not part of the alphabet
        Arguments.of(0, false, "Octet 0 (NUL) is not in the alphabet"), // 'A' – first upper‑case character in Base64 alphabet
        Arguments.of(65, true, "Octet 65 ('A') is in the alphabet"), // '@' – not a Base64 character
        Arguments.of(64, false, "Octet 64 ('@') is not in the alphabet"), // 'z' – lower‑case character, valid
        Arguments.of(122, true, "Octet 122 ('z') is in the alphabet"), // '+' – standard special symbol
        Arguments.of(43, true, "Octet 43 ('+') is in the alphabet"), // DEL (127) – not part of alphabet
        Arguments.of(127, false, "Octet 127 (DEL) is not in the alphabet (entry is -1)"), // Minimum byte value – negative, fails first guard
        Arguments.of(-128, false, "Octet -128 is negative; should be false"), // -1 (0xFF) – negative when interpreted as signed byte
        Arguments.of(-1, false, "Octet -1 (0xFF) is negative; should be false"));
    }

    /**
     * Verify that a byte representing a character that belongs to the Base64 alphabet
     * returns {@code true}.
     */
    @Test
    void testIsInAlphabet_validAlphabetCharacters() {
        // 'A' (65) and 'z' (122) are both part of the standard Base64 alphabet.
        assertTrue(base64.isInAlphabet((byte) 'A'), "Upper‑case A should be in alphabet");
        assertTrue(base64.isInAlphabet((byte) 'z'), "Lower‑case z should be in alphabet");
        // Digit '0' (48) is also in the alphabet.
        assertTrue(base64.isInAlphabet((byte) '0'), "Digit 0 should be in alphabet");
        // '+' (43) and '/' (47) are the two special symbols.
        assertTrue(base64.isInAlphabet((byte) '+'), "Plus sign should be in alphabet");
        assertTrue(base64.isInAlphabet((byte) '/'), "Slash should be in alphabet");
    }

    /**
     * Verify that the padding character '=' is **not** considered part of the alphabet.
     */
    @Test
    void testIsInAlphabet_paddingCharacter() {
        assertFalse(base64.isInAlphabet((byte) '='), "Padding character '=' must not be in alphabet");
    }

    /**
     * Verify that a negative byte value (i.e., a signed byte less than zero) is rejected.
     */
    @Test
    void testIsInAlphabet_negativeByte() {
        // (byte) -1 is 0xFF, which is negative when interpreted as a signed byte.
        assertFalse(base64.isInAlphabet((byte) -1), "Negative byte values must not be in alphabet");
    }

    /**
     * Verify that a byte whose unsigned value is greater than the decode table length
     * is rejected. The standard decode table length is 128; the value 127 is the highest
     * index that exists, but its entry is {@code -1} (non‑alphabet). Using 127 therefore
     * tests the “out‑of‑alphabet but in‑range” path.
     */
    @Test
    void testIsInAlphabet_outOfRangeOrNonAlphabetByte() {
        // 127 is the highest index in the standard decode table and maps to -1.
        assertFalse(base64.isInAlphabet((byte) 127), "Byte 127 should not be in alphabet (decode table entry is -1)");
        // 0x80 as a signed byte becomes -128, which fails the >=0 check.
        assertFalse(base64.isInAlphabet((byte) 0x80), "Byte 0x80 (signed -128) must be rejected by the >=0 guard");
    }

    /**
     * Verify that a custom Base64 instance created via the builder with a non‑standard
     * encode table still correctly reports alphabet membership for its own table.
     * This indirectly tests that the constructor correctly builds a matching decode table.
     */
    @Test
    void testIsInAlphabet_customBuilder() {
        // Create a URL‑safe instance – its encode table differs, but the alphabet check should still work.
        Base64 urlSafe = Base64.builder().setUrlSafe(true).get();
        // In URL‑safe mode, '-' and '_' replace '+' and '/'.
        assertTrue(urlSafe.isInAlphabet((byte) '-'), "URL‑safe '-' should be in alphabet");
        assertTrue(urlSafe.isInAlphabet((byte) '_'), "URL‑safe '_' should be in alphabet");
        // The standard '+' and '/' are no longer part of the URL‑safe alphabet.
        assertFalse(urlSafe.isInAlphabet((byte) '+'), "Standard '+' must not be in URL‑safe alphabet");
        assertFalse(urlSafe.isInAlphabet((byte) '/'), "Standard '/' must not be in URL‑safe alphabet");
    }

    /**
     * Parameterised test that exercises a variety of octet values taken from the
     * supplied JSON test matrix.  Each row contains:
     * <ul>
     *   <li>the octet as an {@code int} (will be cast to {@code byte}),</li>
     *   <li>the expected boolean result,</li>
     *   <li>a short description useful for debugging failures.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: octet={0} ⇒ expected={1} ({2})")
    @MethodSource("provideIsInAlphabetCases")
    void testIsInAlphabet_various(int octet, boolean expected, String description) {
        // cast respects Java's signed byte semantics
        byte b = (byte) octet;
        if (expected) {
            assertTrue(base64.isInAlphabet(b), () -> "Expected TRUE for octet " + octet + " – " + description);
        } else {
            assertFalse(base64.isInAlphabet(b), () -> "Expected FALSE for octet " + octet + " – " + description);
        }
    }

    /**
     * Validates that a URL‑safe {@link Base64} instance adapts its alphabet
     * correctly (replaces '+' and '/' with '-' and '_').
     */
    @Test
    void testIsInAlphabet_urlSafeInstance() {
        Base64 urlSafe = Base64.builder().setUrlSafe(true).get();
        // URL‑safe specific characters.
        assertTrue(urlSafe.isInAlphabet((byte) '-'), "URL‑safe '-' should be in alphabet");
        assertTrue(urlSafe.isInAlphabet((byte) '_'), "URL‑safe '_' should be in alphabet");
        // Standard characters that are *not* part of the URL‑safe alphabet.
        assertFalse(urlSafe.isInAlphabet((byte) '+'), "Standard '+' must not be in URL‑safe alphabet");
        assertFalse(urlSafe.isInAlphabet((byte) '/'), "Standard '/' must not be in URL‑safe alphabet");
    }

    /**
     * Simulates a corrupted internal state where {@code decodeTable} is {@code null}.
     * The method should throw a {@link NullPointerException} when trying to access
     * {@code decodeTable.length}.
     */
    @Test
    void testIsInAlphabet_nullDecodeTable() throws Exception {
        // Use reflection to inject a null decode table.
        Field tableField = Base64.class.getDeclaredField("decodeTable");
        tableField.setAccessible(true);
        tableField.set(base64, null);
        // Any call now triggers NPE because the method accesses decodeTable.length.
        assertThrows(NullPointerException.class, () -> base64.isInAlphabet((byte) 0), "With a null decodeTable, isInAlphabet should throw NullPointerException");
    }
}

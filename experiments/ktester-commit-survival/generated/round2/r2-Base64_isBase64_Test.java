package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class Base64_isBase64_Test {

    /**
     * Supplies all test cases from the JSON document.
     *
     * @return a stream of {@link Arguments} objects
     */
    private static Stream<Arguments> provideIsBase64ByteCases() {
        return Stream.of(// ---- Group: Base64.isBase64(byte) ----
        Arguments.of(61, true, "PAD_DEFAULT ('=') – first OR condition"), Arguments.of(65, true, "Upper‑case 'A' – valid Base64 character"), Arguments.of(122, true, "Lower‑case 'z' – last valid Base64 character"), Arguments.of(32, false, "Space character – in range but maps to -1"), Arguments.of(0, false, "Null byte – in range but maps to -1"), Arguments.of(-1, false, "Negative value – fails octet >= 0 check"), Arguments.of(127, false, "DEL character – in range but maps to -1"), // ---- Group: Base64.isBase64(byte) - additional cases ----
        Arguments.of(43, true, "'+' (ASCII 43) – valid Base64"), Arguments.of(45, true, "'-' (ASCII 45) – URL‑safe variant, maps to 62"), Arguments.of(47, true, "'/' (ASCII 47) – valid Base64"), Arguments.of(95, true, "'_' (ASCII 95) – URL‑safe variant, maps to 63"), Arguments.of(48, true, "'0' (ASCII 48) – digit, maps to 52"), Arguments.of(57, true, "'9' (ASCII 57) – digit, maps to 61"), Arguments.of(64, false, "'@' (ASCII 64) – not in Base64 alphabet"), Arguments.of(91, false, "'[' (ASCII 91) – not in Base64 alphabet"), Arguments.of(93, false, "']' (ASCII 93) – not in Base64 alphabet"), Arguments.of(124, false, "'|' (ASCII 124) – not in Base64 alphabet"), Arguments.of(-128, false, "Minimum byte value (-128) – negative"), Arguments.of(126, false, "'~' (ASCII 126) – not a Base64 character"), Arguments.of(63, false, "'?' (ASCII 63) – not a Base64 character"), Arguments.of(123, false, "'{' (ASCII 123) – not a Base64 character"));
    }

    /**
     * Verify that all characters defined by the Base64 alphabet are recognised as Base64.
     */
    @Test
    void testAlphabetCharactersAreBase64() {
        // Upper‑case A‑Z
        for (char c = 'A'; c <= 'Z'; c++) {
            assertTrue(Base64.isBase64((byte) c), "Expected '" + c + "' to be recognised as Base64");
        }
        // Lower‑case a‑z
        for (char c = 'a'; c <= 'z'; c++) {
            assertTrue(Base64.isBase64((byte) c), "Expected '" + c + "' to be recognised as Base64");
        }
        // Digits 0‑9
        for (char c = '0'; c <= '9'; c++) {
            assertTrue(Base64.isBase64((byte) c), "Expected '" + c + "' to be recognised as Base64");
        }
        // The two special symbols
        assertTrue(Base64.isBase64((byte) '+'), "Expected '+' to be recognised as Base64");
        assertTrue(Base64.isBase64((byte) '/'), "Expected '/' to be recognised as Base64");
        // Padding character
        assertTrue(Base64.isBase64((byte) '='), "Expected '=' (padding) to be recognised as Base64");
    }

    /**
     * Verify that whitespace characters are *not* recognised as Base64 by the
     * {@code isBase64(byte)} method (they are ignored only by the array version).
     */
    @Test
    void testWhitespaceIsNotBase64() {
        byte[] whitespace = { (byte) ' ', (byte) '\t', (byte) '\n', (byte) '\r', (byte) '\f' };
        for (byte b : whitespace) {
            assertFalse(Base64.isBase64(b), "Whitespace character 0x" + Integer.toHexString(b & 0xFF) + " should not be Base64");
        }
    }

    /**
     * Verify that characters outside the Base64 alphabet are not recognised.
     */
    @Test
    void testInvalidCharactersAreNotBase64() {
        char[] invalid = { // '=' is padding, already tested
        '@', // '=' is padding, already tested
        '#', // '=' is padding, already tested
        '$', // '=' is padding, already tested
        '%', // '=' is padding, already tested
        '^', // '=' is padding, already tested
        '&', // '=' is padding, already tested
        '*', // '=' is padding, already tested
        '(', // '=' is padding, already tested
        ')', // '=' is padding, already tested
        '-', // '=' is padding, already tested
        '_', // '=' is padding, already tested
        '=', '[', ']', '{', '}', '|', '\\', ':', ';', '\'', '"', ',', '<', '>', '?', '~', '`' };
        for (char c : invalid) {
            // Skip the padding character which is valid
            if (c == '=') {
                continue;
            }
            assertFalse(Base64.isBase64((byte) c), "Expected '" + c + "' to be NOT recognised as Base64");
        }
    }

    /**
     * Verify that negative byte values are never considered Base64.
     */
    @Test
    void testNegativeBytesAreNotBase64() {
        for (int i = -128; i < 0; i++) {
            byte b = (byte) i;
            // The only possible exception would be if the negative value maps to the
            // padding character (which is 61, i.e. '=') – that cannot happen.
            assertFalse(Base64.isBase64(b), "Negative byte value " + i + " should not be recognised as Base64");
        }
    }

    /**
     * Verify that values beyond the unsigned byte range (i.e. > 127) are handled
     * correctly when cast to {@code byte}. The method works on signed bytes, so
     * values 128‑255 become negative when cast; they must be reported as not
     * Base64 unless they correspond to a valid character after casting.
     */
    @Test
    void testUnsignedByteValuesAfterCasting() {
        // Values 128‑255 that correspond to valid Base64 characters after casting:
        //   128‑255 map to -128‑-1; none of the Base64 alphabet characters have code > 127,
        //   so they should all be false.
        for (int i = 128; i <= 255; i++) {
            byte b = (byte) i;
            assertFalse(Base64.isBase64(b), "Byte value " + i + " (cast to " + b + ") should not be recognised as Base64");
        }
    }

    /**
     * Parameterized test that covers both the original JSON groups:
     * <ul>
     *   <li>"Base64.isBase64(byte)"</li>
     *   <li>"Base64.isBase64(byte) - additional cases"</li>
     * </ul>
     *
     * Each argument consists of:
     * <pre>
     *   octet (int) – the raw integer value that will be cast to a byte,
     *   expected (boolean) – the value we expect from Base64.isBase64,
     *   description (String) – human‑readable explanation (used only in the source).
     * </pre>
     */
    @ParameterizedTest(name = "[{index}] octet={0} → expect={1} ({2})")
    @MethodSource("provideIsBase64ByteCases")
    void testIsBase64ByteParameterized(int octet, boolean expected, String description) {
        // Cast to byte because the method signature expects a signed byte.
        byte b = (byte) octet;
        boolean result = Base64.isBase64(b);
        assertEquals(expected, result, () -> "Failure for octet " + octet + " (" + description + ")");
    }
}

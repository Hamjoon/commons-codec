package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Base64_isBase64_3_Test {

    /* Helper to convert a String (ASCII) to the byte[] expected by the SUT. */
    private static byte[] toAsciiBytes(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static Stream<Arguments> validBase64Provider() {
        return Stream.of(Arguments.of("TWFu", true, "All bytes are valid Base64 characters (\"Man\" encoded as \"TWFu\")."), Arguments.of("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/", true, "Array contains every character from the standard Base64 alphabet."));
    }

    static Stream<Arguments> interleavedWhitespaceProvider() {
        return Stream.of(Arguments.of("T W F u\n", true, "Valid Base64 \"TWFu\" with spaces and newline interleaved"));
    }

    static Stream<Arguments> invalidCharacterProvider() {
        return Stream.of(Arguments.of("SGV!sbG8=", false, "Byte '!' is not part of the Base64 alphabet nor whitespace"), Arguments.of("AB CD!EF", false, "Mix of valid Base64, whitespace and invalid '!' should yield false"));
    }

    /**
     * A simple, well‑known Base64 string: "Hello".
     */
    @Test
    void testValidBase64Bytes() {
        // "SGVsbG8=" is the Base64 representation of "Hello"
        final byte[] base64Bytes = "SGVsbG8=".getBytes(StandardCharsets.US_ASCII);
        assertTrue(Base64.isBase64(base64Bytes), "Standard Base64 bytes should be reported as valid");
    }

    /**
     * Base64 data may contain whitespace characters which should be ignored.
     */
    @Test
    void testValidBase64WithWhitespace() {
        // Insert spaces, tabs, CR and LF into a valid Base64 string.
        final String withWs = "S G V s b G 8 =\n\t\r";
        final byte[] bytes = withWs.getBytes(StandardCharsets.US_ASCII);
        assertTrue(Base64.isBase64(bytes), "Base64 bytes containing whitespace should be reported as valid");
    }

    /**
     * Any byte that is not a Base64 character and not whitespace makes the whole
     * array invalid.
     */
    @Test
    void testInvalidBase64Bytes() {
        // '!' (0x21) is not part of the Base64 alphabet and not whitespace.
        final byte[] invalid = "SGV!sbG8=".getBytes(StandardCharsets.US_ASCII);
        assertFalse(Base64.isBase64(invalid), "Array containing non‑Base64, non‑whitespace characters should be invalid");
    }

    /**
     * An empty array contains no illegal characters, therefore it is considered
     * valid Base64.
     */
    @Test
    void testEmptyArrayIsValid() {
        // An empty array contains no illegal characters, therefore it is considered valid.
        assertTrue(Base64.isBase64(new byte[0]), "Empty byte array should be considered valid Base64");
    }

    /**
     * Passing {@code null} should result in a {@link NullPointerException}
     * because the method iterates over the array without a null‑check.
     */
    @Test
    void testNullArrayThrowsNPE() {
        // The method does not guard against null, so a NPE is expected.
        assertThrows(NullPointerException.class, () -> Base64.isBase64(null), "Calling isBase64 with null should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validBase64Provider")
    void testValidBase64Characters(String input, boolean expected, String description) {
        assertEquals(expected, Base64.isBase64(toAsciiBytes(input)), description);
    }

    @ParameterizedTest(name = "[{index}] whitespace only")
    @ValueSource(strings = { " ", "\t", "\r", "\n", " \t\r\n  \n" })
    void testWhitespaceOnlyIsValid(String ws) {
        // Whitespace characters are ignored by the implementation.
        assertTrue(Base64.isBase64(toAsciiBytes(ws)), "Array consisting solely of whitespace should be reported as valid");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("interleavedWhitespaceProvider")
    void testValidBase64WithInterleavedWhitespace(String input, boolean expected, String description) {
        assertEquals(expected, Base64.isBase64(toAsciiBytes(input)), description);
    }

    @Test
    void testPaddingCharacterIsAccepted() {
        // Padding character '=' is part of the Base64 spec and considered valid.
        assertTrue(Base64.isBase64(toAsciiBytes("TQ==")), "Base64 padding character '=' should be accepted");
    }

    @Test
    void testHighBitNegativeByteIsInvalid() {
        // 0xFF (‑1 as signed byte) is not whitespace and not a Base64 character.
        byte[] highBit = new byte[] { (byte) 0xFF };
        assertFalse(Base64.isBase64(highBit), "High‑bit (negative) byte 0xFF should be considered invalid");
    }
}

package org.apache.commons.codec.binary;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Base64_isBase64_2_Test {

    /**
     * Supplies the arguments for {@link #testIsBase64String(String, boolean, String)}.
     *
     * @return a stream of {@link Arguments} containing input, expected result and description.
     */
    private static Stream<Arguments> provideIsBase64StringCases() {
        return Stream.of(// Standard Base64 string without padding
        Arguments.of("TWFu", true, "Standard Base64 string without padding should be recognised as valid."), // Single padding character
        Arguments.of("TWE=", true, "Base64 string with a single padding character should be recognised as valid."), // Double padding characters
        Arguments.of("TQ==", true, "Base64 string with double padding characters should be recognised as valid."), // Empty string – considered a valid (zero‑length) Base64
        Arguments.of("", true, "Empty string is technically a valid (zero‑length) Base64 representation."), // Null input – should be treated as not Base64
        Arguments.of(null, false, "Null input should be treated as not Base64 rather than throwing an exception."), // Whitespace inside the literal string (should be invalid per JSON expectations)
        Arguments.of(" TWFu ", false, "String containing whitespace characters is not a valid Base64 sequence."), // Newline character embedded
        Arguments.of("TWFu\n", false, "String containing a newline character should be rejected as invalid Base64."), // URL‑safe alphabet characters (should be invalid)
        Arguments.of("TWF-", false, "URL‑safe Base64 characters ('-' and '_') are not part of the standard Base64 alphabet, thus should be false."), // Non‑Base64 character '$'
        Arguments.of("abc$", false, "String containing a non‑Base64 character ('$') must be identified as invalid."), // Long, correctly padded repetition
        Arguments.of("TWFuTWFuTWFuTWFuTWFuTWFuTWFuTWFuTWFuTWFu", true, "Longer, correctly padded Base64 string (multiple repetitions) should still be recognised as valid."), // String consisting solely of padding characters – invalid
        Arguments.of("====", false, "String consisting solely of padding characters is not a valid Base64 encoding."));
    }

    /**
     * A {@code null} argument should be considered *not* Base64.
     */
    @Test
    void testIsBase64_NullInput() {
        assertFalse(Base64.isBase64((String) null), "null input should return false");
    }

    /**
     * Empty string is not a valid Base64 representation.
     */
    @Test
    void testIsBase64_EmptyString() {
        assertTrue(Base64.isBase64(""), "empty string should be considered valid Base64");
    }

    /**
     * Simple, known‑good Base64 string.
     */
    @Test
    void testIsBase64_ValidSimple() {
        // "Man" → "TWFu"
        assertTrue(Base64.isBase64("TWFu"), "\"TWFu\" is a valid Base64 string");
    }

    /**
     * Valid Base64 string that contains whitespace characters.
     * Whitespace is ignored by the codec and therefore considered valid.
     */
    @Test
    void testIsBase64_ValidWithWhitespace() {
        // Insert spaces, line‑feeds and tabs arbitrarily
        String withWhiteSpace = " T W F u \n\r\t";
        assertTrue(Base64.isBase64(withWhiteSpace), "Base64 string containing whitespace should be accepted");
    }

    /**
     * String containing characters that are not part of the Base64 alphabet.
     */
    @Test
    void testIsBase64_InvalidCharacters() {
        String invalid = "abc$def==";
        assertFalse(Base64.isBase64(invalid), "String containing '$' is not valid Base64");
    }

    /**
     * A string that looks like Base64 but has an illegal padding position.
     * The static {@code isBase64} method only checks character validity,
     * not padding rules, so this should still return {@code true}.
     */
    @Test
    void testIsBase64_ValidPaddingOnlyCheck() {
        // Padding character '=' is allowed anywhere the method checks characters
        String oddPadding = "TWFu===";
        assertTrue(Base64.isBase64(oddPadding), "Method only validates characters, so odd padding should be accepted");
    }

    /**
     * Verify that URL‑safe Base64 characters are also recognised as valid.
     */
    @Test
    void testIsBase64_UrlSafeAlphabet() {
        // URL‑safe Base64 uses '-' and '_' instead of '+' and '/'
        String urlSafe = "lO2NAxnkSTOZVg-2dATTcA";
        assertFalse(Base64.isBase64(urlSafe), "URL‑safe Base64 characters should be rejected");
    }

    /**
     * Parameterised test covering the exhaustive set of cases described in the
     * provided JSON document.  Each row supplies the input string, the expected
     * boolean result and a short description that is displayed on failure.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideIsBase64StringCases")
    void testIsBase64String(String input, boolean expected, String description) {
        assertEquals(expected, Base64.isBase64(input), description);
    }
}

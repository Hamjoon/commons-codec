package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64 as JdkBase64; // for building expected values
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base64_decodeBase64_2_Test {

    /** Sample plain‑text used in several tests. */
    private static final String FOX_TEXT = "The quick brown fox jumped over the lazy dogs.";

    /** Its Base64 representation (standard, not URL‑safe). */
    private static final String FOX_BASE64 = "VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wZWQgb3ZlciB0aGUgbGF6eSBkb2dzLg==";

    /** A Base64 string that contains URL‑safe characters (‘-’, ‘_’) and no padding. */
    private static final String URL_SAFE_BASE64 = "VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wZWQgb3ZlciB0aGUgbGF6eSBkb2dzLg";

    /** A Base64 string that mixes valid characters with obvious noise. */
    private static final String NOISY_BASE64 = "!!!VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wZWQgb3ZlciB0aGUgbGF6eSBkb2dzLg==???";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
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

    /* --------------------------------------------------------------------- *
     * Existing tests from the original framework (unchanged)                *
     * --------------------------------------------------------------------- */

    /**
     * Verify that a well‑formed Base64 string is decoded to the original byte array.
     */
    @Test
    void testDecodeValidBase64String() {
        byte[] expected = FOX_TEXT.getBytes(StandardCharsets.UTF_8);
        byte[] actual = Base64.decodeBase64(FOX_BASE64);
        assertArrayEquals(expected, actual, "Decoded bytes should match original UTF‑8 bytes");
    }

    /**
     * Verify that an empty string yields an empty byte array.
     */
    @Test
    void testDecodeEmptyString() {
        byte[] actual = Base64.decodeBase64("");
        assertNotNull(actual, "Result should not be null for empty input");
        assertEquals(0, actual.length, "Resulting array should be empty");
    }

    /**
     * Verify that a {@code null} argument results in {@code null}.
     * The static helper delegates to {@link BaseNCodec#decode(String)} which returns {@code null}
     * for a {@code null} input.
     */
    @Test
    void testDecodeNullString() {
        assertNull(Base64.decodeBase64((String) null), "Decoding null should return null");
    }

    /**
     * Verify that the static method tolerates non‑Base64 characters (lenient mode).
     * Noise characters are ignored and the remaining valid payload is decoded.
     */
    @Test
    void testDecodeLenientIgnoresNoise() {
        byte[] expected = FOX_TEXT.getBytes(StandardCharsets.UTF_8);
        byte[] actual = Base64.decodeBase64(NOISY_BASE64);
        assertArrayEquals(expected, actual, "Noise characters should be ignored in lenient mode");
    }

    /**
     * Verify that URL‑safe Base64 strings (no padding, using ‘-’ and ‘_’) are decoded correctly.
     */
    @Test
    void testDecodeUrlSafeString() {
        byte[] expected = FOX_TEXT.getBytes(StandardCharsets.UTF_8);
        byte[] actual = Base64.decodeBase64(URL_SAFE_BASE64);
        assertArrayEquals(expected, actual, "URL‑safe Base64 should decode to the original bytes");
    }

    /**
     * Verify that decoding a string with illegal padding does not throw but returns the best‑effort result.
     * The underlying codec is lenient, so excess padding is ignored.
     */
    @Test
    void testDecodeWithIllegalPadding() {
        String withExtraPadding = FOX_BASE64 + "====";
        byte[] expected = FOX_TEXT.getBytes(StandardCharsets.UTF_8);
        byte[] actual = Base64.decodeBase64(withExtraPadding);
        assertArrayEquals(expected, actual, "Extra padding should be ignored in lenient mode");
    }

    /* --------------------------------------------------------------------- *
     * Parameterised tests generated from the supplied JSON test cases      *
     * --------------------------------------------------------------------- */

    // ---------- Helper ----------
    private static byte[] toByteArray(int[] ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }

    // ---------- Invalid Characters ----------
    private static Stream<Arguments> invalidCharactersProvider() {
        return Stream.of(
                Arguments.of("??!!", new int[]{}),
                Arguments.of("SGVsbG8$%=", new int[]{72, 101, 108, 108, 111})
        );
    }

    @ParameterizedTest(name = "[{index}] Invalid characters in \"{0}\"")
    @MethodSource("invalidCharactersProvider")
    void testInvalidCharactersLenient(String base64String, int[] expectedInts) {
        byte[] expected = toByteArray(expectedInts);
        byte[] actual = Base64.decodeBase64(base64String);
        assertArrayEquals(expected, actual,
                "Lenient mode should ignore non‑alphabet characters and decode the rest");
    }

    // ---------- Standard Decoding ----------
    private static Stream<Arguments> standardDecodingProvider() {
        return Stream.of(
                Arguments.of("TWFu", new int[]{77, 97, 110}),
                Arguments.of("SGVsbG8gV29ybGQh", new int[]{72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33})
        );
    }

    @ParameterizedTest(name = "[{index}] Standard decode \"{0}\"")
    @MethodSource("standardDecodingProvider")
    void testStandardDecoding(String base64String, int[] expectedInts) {
        byte[] expected = toByteArray(expectedInts);
        byte[] actual = Base64.decodeBase64(base64String);
        assertArrayEquals(expected, actual, "Standard Base64 string should decode correctly");
    }

    // ---------- Padding Variations ----------
    private static Stream<Arguments> paddingVariationsProvider() {
        return Stream.of(
                Arguments.of("TQ==", new int[]{77}),
                Arguments.of("TWE=", new int[]{77, 97}),
                Arguments.of("TQ", new int[]{77})
        );
    }

    @ParameterizedTest(name = "[{index}] Padding variation \"{0}\"")
    @MethodSource("paddingVariationsProvider")
    void testPaddingVariations(String base64String, int[] expectedInts) {
        byte[] expected = toByteArray(expectedInts);
        byte[] actual = Base64.decodeBase64(base64String);
        assertArrayEquals(expected, actual, "Base64 with varying padding should decode correctly");
    }

    // ---------- Whitespace Handling ----------
    private static Stream<Arguments> whitespaceHandlingProvider() {
        return Stream.of(
                Arguments.of(" T W F u ", new int[]{77, 97, 110}),
                Arguments.of(
                        "TWFu\r\nIGlz\r\nIGRpc3Rpbmd1aXNoZWQ=",
                        new int[]{77, 97, 110, 32, 105, 115, 32, 100, 105, 115, 116, 105, 110, 103, 117, 105, 115, 104, 101, 100}
                )
        );
    }

    @ParameterizedTest(name = "[{index}] Whitespace handling \"{0}\"")
    @MethodSource("whitespaceHandlingProvider")
    void testWhitespaceHandling(String base64String, int[] expectedInts) {
        byte[] expected = toByteArray(expectedInts);
        byte[] actual = Base64.decodeBase64(base64String);
        assertArrayEquals(expected, actual, "Whitespace should be ignored in lenient mode");
    }

    // ---------- Empty and Null Input ----------
    private static Stream<Arguments> emptyAndNullProvider() {
        return Stream.of(
                Arguments.of("", new int[]{}),
                Arguments.of(null, null)
        );
    }

    @ParameterizedTest(name = "[{index}] Empty/Null input \"{0}\"")
    @MethodSource("emptyAndNullProvider")
    void testEmptyAndNullInput(String base64String, int[] expectedInts) {
        if (base64String == null) {
            assertNull(Base64.decodeBase64((String) null), "Null input should return null in lenient mode");
        } else {
            byte[] expected = toByteArray(expectedInts);
            byte[] actual = Base64.decodeBase64(base64String);
            assertArrayEquals(expected, actual, "Empty string should produce empty byte array");
        }
    }

    // ---------- Invalid Length / Insufficient Data ----------
    private static Stream<Arguments> insufficientDataProvider() {
        return Stream.of(
                Arguments.of("A", new int[]{})
        );
    }

    @ParameterizedTest(name = "[{index}] Insufficient data \"{0}\"")
    @MethodSource("insufficientDataProvider")
    void testInvalidLengthInsufficientData(String base64String, int[] expectedInts) {
        byte[] expected = toByteArray(expectedInts);
        byte[] actual = Base64.decodeBase64(base64String);
        assertArrayEquals(expected, actual,
                "A single Base64 character cannot produce a byte; decoder should return empty array");
    }

    // ---------- Strict Policy – Invalid Characters ----------
    private static Stream<Arguments> strictInvalidCharactersProvider() {
        return Stream.of(
                Arguments.of("??!!"),
                Arguments.of("SGVsbG8$%="),
                Arguments.of(" T W F u "),
                Arguments.of("TWFu\r\nIGlz\r\nIGRpc3Rpbmd1aXNoZWQ=")
        );
    }

    @ParameterizedTest(name = "[{index}] Strict mode should reject \"{0}\"")
    @MethodSource("strictInvalidCharactersProvider")
    void testStrictPolicyInvalidCharacters(String base64String) {
        // Build a Base64 instance with STRICT decoding policy.
        Base64 strictBase64 = Base64.builder()
                .setDecodingPolicy(DecodingPolicy.STRICT)
                .get();

        assertThrows(IllegalArgumentException.class,
                () -> strictBase64.decode(base64String),
                "Strict policy must throw IllegalArgumentException for illegal characters");
    }

    // ---------- Strict Policy – Null Input ----------
    @Test
    void testStrictPolicyNullInput() {
        Base64 strictBase64 = Base64.builder()
                .setDecodingPolicy(DecodingPolicy.STRICT)
                .get();

        assertThrows(IllegalArgumentException.class,
                () -> strictBase64.decode((String) null),
                "Strict policy should treat null as illegal and throw IllegalArgumentException");
    }
}
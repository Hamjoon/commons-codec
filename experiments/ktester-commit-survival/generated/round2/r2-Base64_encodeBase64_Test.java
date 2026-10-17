package org.apache.commons.codec.binary;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64 as JdkBase64; // only for expected values
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link Base64#encodeBase64(byte[])}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Null and empty inputs.</li>
 *   <li>Standard encoding without chunking or URL‑safe mode.</li>
 *   <li>Padding handling (single and double padding).</li>
 *   <li>Non‑ASCII / binary data (signed byte handling).</li>
 *   <li>Minimal inputs that trigger each padding branch.</li>
 *   <li>Non‑multiple‑of‑3 longer inputs.</li>
 *   <li>Large input to verify buffer growth and absence of line‑breaks.</li>
 *   <li>Immutability – the returned array must not be the same instance and must not change when the source is mutated.</li>
 * </ul>
 * No external resources or complex dependencies are required, therefore the
 * {@code @BeforeAll}/{@code @BeforeEach}/{@code @AfterEach}/{@code @AfterAll}
 * methods remain empty but are kept for future extensions.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for the current tests.
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
     *  Existing simple tests (kept unchanged)                               *
     * --------------------------------------------------------------------- */

    /**
     * Verify that a known input is encoded to the expected Base64 representation.
     */
    @Test
    void testEncodeBase64_KnownString() {
        final byte[] input = "Hello".getBytes(StandardCharsets.UTF_8);
        final byte[] expected = "SGVsbG8=".getBytes(StandardCharsets.UTF_8);
        final byte[] actual = Base64.encodeBase64(input);
        assertArrayEquals(expected, actual, "Base64 encoding of \"Hello\" should match RFC 4648");
    }

    /**
     * Encoding an empty byte array should return an empty array (not {@code null}).
     */
    @Test
    void testEncodeBase64_EmptyArray() {
        final byte[] input = new byte[0];
        final byte[] expected = new byte[0];
        final byte[] actual = Base64.encodeBase64(input);
        assertNotNull(actual, "Result must not be null for empty input");
        assertArrayEquals(expected, actual, "Encoding an empty array should yield an empty array");
    }

    /**
     * Encoding {@code null} must return {@code null} according to the contract of the method.
     */
    @Test
    void testEncodeBase64_NullInput() {
        final byte[] actual = Base64.encodeBase64(null);
        assertNull(actual, "Encoding null should return null");
    }

    /**
     * Verify that the method delegates correctly to the overloaded version with the
     * default {@code isChunked = false}. The result should be identical to the direct call.
     */
    @Test
    void testEncodeBase64_DelegatesToOverload() {
        final byte[] input = "Apache Commons Codec".getBytes(StandardCharsets.UTF_8);
        final byte[] expected = Base64.encodeBase64(input, false);
        final byte[] actual = Base64.encodeBase64(input);
        assertArrayEquals(expected, actual,
                "encodeBase64(byte[]) must behave like encodeBase64(byte[], false)");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests covering the JSON test‑case matrix                *
     * --------------------------------------------------------------------- */

    /** Provider for null and empty input cases. */
    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(
                Arguments.of(null, null, "null input should yield null"),
                Arguments.of(new byte[0], new byte[0], "empty input should yield empty array")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullAndEmptyProvider")
    void testEncodeBase64_NullAndEmpty(byte[] input, byte[] expected, String description) {
        byte[] actual = Base64.encodeBase64(input);
        if (expected == null) {
            assertNull(actual, description);
        } else {
            assertNotNull(actual, description);
            assertArrayEquals(expected, actual, description);
        }
    }

    /** Provider for standard (non‑chunked, non‑URL‑safe) encoding cases. */
    private static Stream<Arguments> standardEncodingProvider() {
        return Stream.of(
                Arguments.of(new byte[]{97, 98, 99}, // "abc"
                        JdkBase64.getEncoder().encode(new byte[]{97, 98, 99}),
                        "Standard encoding of \"abc\""),
                Arguments.of("foobar".getBytes(StandardCharsets.UTF_8),
                        JdkBase64.getEncoder().encode("foobar".getBytes(StandardCharsets.UTF_8)),
                        "Standard encoding of \"foobar\"")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("standardEncodingProvider")
    void testEncodeBase64_Standard(byte[] input, byte[] expected, String description) {
        byte[] actual = Base64.encodeBase64(input);
        assertArrayEquals(expected, actual, description);
    }

    /** Provider for padding handling cases. */
    private static Stream<Arguments> paddingProvider() {
        return Stream.of(
                Arguments.of(new byte[]{97, 110, 121}, // "any"
                        JdkBase64.getEncoder().encode(new byte[]{97, 110, 121}),
                        "Padding with two '=' characters (\"any\")"),
                Arguments.of("pleasure".getBytes(StandardCharsets.UTF_8),
                        JdkBase64.getEncoder().encode("pleasure".getBytes(StandardCharsets.UTF_8)),
                        "Single padding character (\"pleasure\")")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("paddingProvider")
    void testEncodeBase64_Padding(byte[] input, byte[] expected, String description) {
        byte[] actual = Base64.encodeBase64(input);
        assertArrayEquals(expected, actual, description);
    }

    /** Provider for non‑ASCII / binary data. */
    private static Stream<Arguments> binaryDataProvider() {
        return Stream.of(
                Arguments.of(new byte[]{0, -1, 127, -128, 34, 56},
                        JdkBase64.getEncoder().encode(new byte[]{0, -1, 127, -128, 34, 56}),
                        "Binary data containing negative values")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("binaryDataProvider")
    void testEncodeBase64_NonAsciiBinary(byte[] input, byte[] expected, String description) {
        byte[] actual = Base64.encodeBase64(input);
        assertArrayEquals(expected, actual, description);
    }

    /** Provider for minimal inputs that trigger each padding branch. */
    private static Stream<Arguments> minimalInputProvider() {
        return Stream.of(
                Arguments.of(new byte[]{0},
                        JdkBase64.getEncoder().encode(new byte[]{0}),
                        "Single zero byte → \"AA==\""),
                Arguments.of(new byte[]{(byte) 0xFF},
                        JdkBase64.getEncoder().encode(new byte[]{(byte) 0xFF}),
                        "Single 0xFF byte → \"/w==\""),
                Arguments.of(new byte[]{0, 1},
                        JdkBase64.getEncoder().encode(new byte[]{0, 1}),
                        "Two‑byte array → \"AAE=\"")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("minimalInputProvider")
    void testEncodeBase64_MinimalInput(byte[] input, byte[] expected, String description) {
        byte[] actual = Base64.encodeBase64(input);
        assertArrayEquals(expected, actual, description);
    }

    /** Provider for non‑multiple‑of‑3 longer inputs. */
    private static Stream<Arguments> nonMultipleOfThreeProvider() {
        return Stream.of(
                Arguments.of(new byte[]{1, 2, 3, 4},
                        JdkBase64.getEncoder().encode(new byte[]{1, 2, 3, 4}),
                        "Four bytes → \"AQIDBA==\""),
                Arguments.of(new byte[]{1, 2, 3, 4, 5},
                        JdkBase64.getEncoder().encode(new byte[]{1, 2, 3, 4, 5}),
                        "Five bytes → \"AQIDBAU=\"")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonMultipleOfThreeProvider")
    void testEncodeBase64_NonMultipleOfThree(byte[] input, byte[] expected, String description) {
        byte[] actual = Base64.encodeBase64(input);
        assertArrayEquals(expected, actual, description);
    }

    /** Test for a large input (100 sequential bytes) – verifies no line breaks and correct length. */
    @Test
    void testEncodeBase64_LargeInput_NoChunking() {
        // Build an input array containing values 0..99
        byte[] input = new byte[100];
        for (int i = 0; i < 100; i++) {
            input[i] = (byte) i;
        }

        // Expected result using JDK's Base64 encoder (which does not insert CR/LF by default)
        byte[] expected = JdkBase64.getEncoder().encode(input);

        // When
        byte[] actual = Base64.encodeBase64(input);

        // Then
        assertArrayEquals(expected, actual, "Large input should be encoded correctly without chunking");
        assertEquals(136, actual.length, "Base64 of 100 bytes should be 136 bytes long");
        // Ensure no CR (13) or LF (10) characters are present
        for (byte b : actual) {
            assertFalse(b == '\r' || b == '\n',
                    "Result must not contain CR/LF characters");
        }
    }

    /** Immutability test – the returned array must be a new instance and must not change when the source is mutated. */
    @Test
    void testEncodeBase64_Immutability() {
        // Original data representing "abc"
        byte[] original = new byte[]{97, 98, 99};

        // Encode
        byte[] encoded = Base64.encodeBase64(original);

        // Mutate the original array after encoding
        original[0] = 0; // change 'a' (97) to 0

        // Expected result should still be the encoding of the *original* data, i.e. "YWJj"
        byte[] expected = JdkBase64.getEncoder().encode(new byte[]{97, 98, 99});

        // Verify that the encoded array is unchanged
        assertArrayEquals(expected, encoded,
                "Encoded result must remain equal to the encoding of the original data even after the source is mutated");

        // Verify that a new array instance is returned (not the same reference)
        assertNotSame(original, encoded,
                "Returned byte array must be a different instance from the input array");
    }
}
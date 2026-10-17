package org.apache.commons.codec.binary;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64 as JdkBase64;          // reference encoder/decoder (JDK)
import java.util.stream.Stream;

import org.apache.commons.codec.DecodingPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link Base64#decodeBase64(byte[])} covering the
 * exhaustive test matrix supplied in the JSON description.
 *
 * <p>
 * The test class contains:
 * <ul>
 *   <li>ordinary (lenient) decoding tests – executed via the static
 *       {@code Base64.decodeBase64(byte[])} method;</li>
 *   <li>strict‑mode decoding tests – executed via a {@code Base64}
 *       instance built with {@link DecodingPolicy#STRICT};</li>
 *   <li>an additional mutation‑safety test that guarantees the returned
 *       array is a copy.</li>
 * </ul>
 * </p>
 */
class Base64_decodeBase64_Test {

    /** Sample text used by the original hand‑written tests (kept for completeness). */
    private static final String SAMPLE_TEXT = "Apache Commons Codec – Base64 test data";

    private byte[] sampleBytes;
    private byte[] jdkEncoded;
    private byte[] jdkUrlSafeEncoded;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        sampleBytes = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        jdkEncoded = JdkBase64.getEncoder().encode(sampleBytes);
        jdkUrlSafeEncoded = JdkBase64.getUrlEncoder().withoutPadding().encode(sampleBytes);
    }

    @AfterEach
    void teardownAfterEach() {
        sampleBytes = null;
        jdkEncoded = null;
        jdkUrlSafeEncoded = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /* --------------------------------------------------------------------- */
    /*  Original hand‑written tests (kept unchanged)                        */
    /* --------------------------------------------------------------------- */

    /** Standard round‑trip using the static API. */
    @Test
    void testDecodeBase64_StandardData() {
        byte[] encoded = Base64.encodeBase64(sampleBytes);
        byte[] decoded = Base64.decodeBase64(encoded);
        assertArrayEquals(sampleBytes, decoded,
                "Decoded bytes should match the original input");
    }

    /** Empty input must yield an empty array (not {@code null}). */
    @Test
    void testDecodeBase64_EmptyArray() {
        byte[] empty = new byte[0];
        byte[] result = Base64.decodeBase64(empty);
        assertNotNull(result, "Result should not be null for empty input");
        assertEquals(0, result.length, "Result length should be zero for empty input");
    }

    /** Null input is defined to return {@code null}. */
    @Test
    void testDecodeBase64_NullInput() {
        assertNull(Base64.decodeBase64(null),
                "Decoding a null reference should return null");
    }

    /** URL‑safe, no‑padding data must be decoded correctly. */
    @Test
    void testDecodeBase64_UrlSafeNoPadding() {
        byte[] decoded = Base64.decodeBase64(jdkUrlSafeEncoded);
        assertArrayEquals(sampleBytes, decoded,
                "URL‑safe decoded bytes should match the original input");
    }

    /** Whitespace characters are ignored by the default (lenient) decoder. */
    @Test
    void testDecodeBase64_WithWhitespace() {
        String withWs = new String(jdkEncoded, StandardCharsets.US_ASCII)
                .replaceAll("(.{4})", "$1\n\t "); // inject newline, tab, space
        byte[] decoded = Base64.decodeBase64(withWs.getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(sampleBytes, decoded,
                "Decoder should ignore whitespace and return the original bytes");
    }

    /** Illegal characters are skipped in lenient mode. */
    @Test
    void testDecodeBase64_WithIllegalCharacters() {
        String valid = new String(jdkEncoded, StandardCharsets.US_ASCII);
        String polluted = valid.replace('A', '@')
                               .replace('B', '#')
                               .replace('C', '$');
        byte[] decoded = Base64.decodeBase64(polluted.getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(sampleBytes, decoded,
                "Lenient decoder should ignore non‑Base64 characters and decode the rest");
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests generated from the JSON matrix                    */
    /* --------------------------------------------------------------------- */

    /** Lenient (default) decoding test cases. */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("lenientTestCases")
    void testDecodeBase64_Lenient(String description, byte[] input, Object expected) {
        byte[] result = Base64.decodeBase64(input);
        if (expected == null) {
            assertNull(result, description);
        } else if (expected instanceof byte[]) {
            assertArrayEquals((byte[]) expected, result, description);
        } else {
            fail("Unexpected expected value type for test: " + description);
        }
    }

    /** Strict‑mode decoding test cases – must throw {@link IllegalArgumentException}. */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("strictTestCases")
    void testDecodeBase64_Strict(String description, byte[] input, Class<? extends Throwable> expectedException) {
        Base64 strictBase64 = Base64.builder()
                                    .setDecodingPolicy(DecodingPolicy.STRICT)
                                    .get();
        assertThrows(expectedException, () -> strictBase64.decode(input), description);
    }

    /** Mutation‑safety test – the returned array must be independent of the input array. */
    @Test
    void testDecodeBase64_InputMutationSafety() {
        byte[] input = "AQIDBAU=".getBytes(StandardCharsets.US_ASCII);
        byte[] result = Base64.decodeBase64(input);
        // Mutate the original input after decoding.
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) ~input[i];
        }
        // Expected bytes are 1,2,3,4,5.
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, result,
                "Decoded result must not be affected by later changes to the input array");
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods for building the argument streams                      */
    /* --------------------------------------------------------------------- */

    /** Builds the argument stream for all lenient test cases. */
    private static Stream<Arguments> lenientTestCases() {
        return Stream.of(
                // Null and empty input
                Arguments.of("Null input returns null", null, null),
                Arguments.of("Empty array returns empty array", new byte[0], new byte[0]),

                // Valid Base64 decoding (no padding, no whitespace)
                Arguments.of("Hello World! (no padding)", toBytes("SGVsbG8gV29ybGQh"),
                        new byte[]{72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33}),
                Arguments.of("Simple numeric bytes 0x01 0x02 0x03", toBytes("AQID"),
                        new byte[]{1, 2, 3}),

                // Padding handling
                Arguments.of("Two padding characters (==)", toBytes("Zm9vYg=="),
                        new byte[]{102, 111, 111, 98}),
                Arguments.of("Single padding character (=)", toBytes("Zm9vYw="),
                        new byte[]{102, 111, 111, 99}),

                // Whitespace and line separator handling
                Arguments.of("Spaces inside Base64 string", toBytes(" SG V s b G 8 g V 2 9 y b G Q h "),
                        new byte[]{72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33}),
                Arguments.of("CR/LF line separators", toBytes("SGVsbG8g\r\nV29ybGQh"),
                        new byte[]{72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33}),

                // Invalid characters handling
                Arguments.of("Illegal character '@' inside string", toBytes("SGVsbG8gV@29ybGQh"),
                        new byte[]{72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33}),
                Arguments.of("String with only illegal characters", toBytes("!!!"),
                        new byte[0]),

                // Large input / chunked output
                Arguments.of("Long string (multiple 76‑char lines)", toBytes(
                        "VGhpcyBpcyBhIHZlcnkgbG9uZyBzdHJpbmcgdGhhdCBpcyB0byBiZSBkZWNvZGVkIGluIG11bHRpcGxlIGJsb2NrcyB0byB0ZXN0IHRoZSBjaGFyYWN0ZXJpc2l0eSBvZiB0aGUgZGVjb2RlIG1ldGhvZC4="),
                        JdkBase64.getDecoder().decode(
                                "VGhpcyBpcyBhIHZlcnkgbG9uZyBzdHJpbmcgdGhhdCBpcyB0byBiZSBkZWNvZGVkIGluIG11bHRpcGxlIGJsb2NrcyB0byB0ZXN0IHRoZSBjaGFyYWN0ZXJpc2l0eSBvZiB0aGUgZGVjb2RlIG1ldGhvZC4=")),

                // Missing padding handling
                Arguments.of("Missing padding (no '=')", toBytes("YWJjZg"),
                        new byte[]{97, 98, 99}),

                // Round‑trip binary data (0‑15)
                Arguments.of("Full range 0‑15", toBytes("AAECAwQFBgcICQoLDA0ODw=="),
                        new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}),

                // Non‑ASCII input bytes (should be ignored → empty)
                Arguments.of("Non‑ASCII characters (éâß)", toBytes("éâß"),
                        new byte[0]),

                // Mixed valid/invalid characters
                Arguments.of("Mixed valid/invalid (A!B?C D)", toBytes("A!B?C D"),
                        new byte[]{0, 16, 35}),

                // Whitespace variants (tabs, CR, LF, form‑feed)
                Arguments.of("Various whitespace chars", toBytes("\tSGVs bG8g\r\nV29y bGQh\u000c"),
                        new byte[]{72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33}),

                // Invalid padding position (lenient mode stops at first '=').
                Arguments.of("Invalid padding position (early '=')", toBytes("YWJj=Zg=="),
                        new byte[]{97, 98, 99})
        );
    }

    /** Builds the argument stream for strict‑mode test cases. */
    private static Stream<Arguments> strictTestCases() {
        return Stream.of(
                Arguments.of("Strict mode – illegal character '@'",
                        toBytes("SGVsbG8gV@29ybGQh"),
                        IllegalArgumentException.class),
                Arguments.of("Strict mode – early padding character",
                        toBytes("YWJj=Zg=="),
                        IllegalArgumentException.class),
                Arguments.of("Strict mode – incorrect padding length",
                        toBytes("Zm9vYg="),
                        IllegalArgumentException.class),
                Arguments.of("Strict mode – non‑ASCII bytes (>127)",
                        toBytes("éâß"),
                        IllegalArgumentException.class)
        );
    }

    /** Convenience helper – converts a literal Base64 string to its US‑ASCII byte representation. */
    private static byte[] toBytes(String base64) {
        return base64.getBytes(StandardCharsets.US_ASCII);
    }
}
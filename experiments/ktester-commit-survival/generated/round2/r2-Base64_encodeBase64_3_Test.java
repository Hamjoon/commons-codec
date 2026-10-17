package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64 as JdkBase64; // alias to avoid confusion with our class
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit‑tests for {@link Base64#encodeBase64(byte[], boolean, boolean)}.
 *
 * The test suite combines:
 * <ul>
 *   <li>hand‑written tests that were already present in the repository</li>
 *   <li>parameterised tests generated from the JSON test matrix supplied with the kata</li>
 * </ul>
 *
 * No external dependencies are required – the class under test is static and self‑contained.
 */
@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64_3_Test {

    /** Sample data used by several tests. */
    private static final byte[] SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

    /** Expected Base64 representation of {@link #SAMPLE_TEXT} using the JDK encoder (standard, no line‑breaks). */
    private static final byte[] EXPECTED_STANDARD = JdkBase64.getEncoder().encode(SAMPLE_TEXT);

    /** Expected Base64 URL‑safe representation of {@link #SAMPLE_TEXT} (no padding, no line‑breaks). */
    private static final byte[] EXPECTED_URLSAFE = JdkBase64.getUrlEncoder().withoutPadding().encode(SAMPLE_TEXT);

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these static tests.
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

    /* --------------------------------------------------------------------- */
    /* Existing explicit tests (kept unchanged)                             */
    /* --------------------------------------------------------------------- */

    /**
     * Verifies that encoding without chunking and without URL‑safe mode matches the JDK
     * standard Base64 output.
     */
    @Test
    void encodeBase64_standard_noChunk_noUrlSafe() {
        byte[] result = Base64.encodeBase64(SAMPLE_TEXT, false, false);
        assertArrayEquals(EXPECTED_STANDARD, result,
                "Standard Base64 encoding should match JDK encoder when chunking is disabled and URL‑safe is false");
    }

    /**
     * Verifies that encoding with URL‑safe mode produces the same output as the JDK
     * URL‑safe encoder (without padding).
     */
    @Test
    void encodeBase64_urlSafe_noChunk() {
        byte[] result = Base64.encodeBase64(SAMPLE_TEXT, false, true);
        assertArrayEquals(EXPECTED_URLSAFE, result,
                "URL‑safe Base64 encoding should match JDK URL‑safe encoder when chunking is disabled");
    }

    /**
     * When {@code isChunked} is {@code true} the implementation inserts CRLF
     * line separators every 76 characters (the default MIME line length). For a
     * short input the result should be identical to the non‑chunked version.
     */
    @Test
    void encodeBase64_chunked_smallInput() {
        byte[] result = Base64.encodeBase64(SAMPLE_TEXT, true, false);
        // Input length is far below the 76‑character line limit, so the output must be identical.
        assertArrayEquals(EXPECTED_STANDARD, result,
                "Chunked encoding of a short payload must not introduce line breaks");
    }

    /**
     * An empty input array should result in an empty output array, regardless of
     * the flags.
     */
    @Test
    void encodeBase64_emptyInput() {
        byte[] empty = new byte[0];
        assertArrayEquals(new byte[0], Base64.encodeBase64(empty, false, false));
        assertArrayEquals(new byte[0], Base64.encodeBase64(empty, true, true));
    }

    /**
     * Passing {@code null} should return {@code null} – this mirrors the behaviour
     * of the original Apache Commons Codec implementation.
     */
    @Test
    void encodeBase64_nullInput() {
        assertNull(Base64.encodeBase64(null, false, false));
        assertNull(Base64.encodeBase64(null, true, true));
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised tests generated from the supplied JSON matrix           */
    /* --------------------------------------------------------------------- */

    /**
     * Parameterised test that drives all cases from the JSON description.
     *
     * @param description textual description (used only for the test name)
     * @param binaryData  the source byte array – may be a literal array or the special marker "zeroes-76"
     * @param isChunked   whether chunked output is requested
     * @param urlSafe     whether URL‑safe alphabet should be used
     * @param expected    the expected Base64 string, or {@code null} when a {@code null} result is expected
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideEncodeBase64Cases")
    void encodeBase64_parameterised(String description,
                                    Object binaryData,
                                    boolean isChunked,
                                    boolean urlSafe,
                                    String expected) {

        byte[] input = resolveBinaryData(binaryData);
        byte[] result = Base64.encodeBase64(input, isChunked, urlSafe);

        if (expected == null) {
            assertNull(result, description);
        } else {
            byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
            assertArrayEquals(expectedBytes, result, description);
        }
    }

    /**
     * Converts the heterogeneous {@code binaryData} value coming from the JSON
     * matrix into a real {@code byte[]} that can be fed to the method under test.
     *
     * <ul>
     *   <li>If the value is a {@code String} equal to {@code "zeroes-76"} we create
     *       an array with 76 zero bytes (used for the chunked tests).</li>
     *   <li>If the value is a {@code java.util.List<Integer>} we turn it into a
     *       {@code byte[]} preserving the signedness of each element.</li>
     *   <li>If the value is {@code null} we simply return {@code null}.</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private static byte[] resolveBinaryData(Object binaryData) {
        if (binaryData == null) {
            return null;
        }
        if (binaryData instanceof String) {
            String marker = (String) binaryData;
            if ("zeroes-76".equals(marker)) {
                return new byte[76]; // all bytes are already zero‑initialised
            }
            throw new IllegalArgumentException("Unsupported string marker: " + marker);
        }
        // JSON numbers are deserialized as Integer objects in a java.util.List
        if (binaryData instanceof java.util.List) {
            java.util.List<Integer> ints = (java.util.List<Integer>) binaryData;
            byte[] bytes = new byte[ints.size()];
            for (int i = 0; i < ints.size(); i++) {
                bytes[i] = (byte) (ints.get(i) & 0xFF);
            }
            return bytes;
        }
        throw new IllegalArgumentException("Unsupported binaryData type: " + binaryData.getClass());
    }

    /**
     * Supplies the test data extracted from the JSON matrix.
     *
     * Each {@link Arguments} line corresponds to one row in the original JSON.
     * The order is kept so that the generated test names are stable and easy to
     * map back to the source description.
     */
    private static Stream<Arguments> provideEncodeBase64Cases() {
        return Stream.of(
                // Null Input
                Arguments.of(
                        "When the source byte array is null the method should return null regardless of the flags.",
                        null,
                        false,
                        false,
                        null),

                // Empty Input
                Arguments.of(
                        "Encoding an empty byte array should return an empty byte array (empty string) regardless of chunking or URL‑safe flags.",
                        java.util.Collections.emptyList(),
                        false,
                        false,
                        ""),

                // Single Byte Standard
                Arguments.of(
                        "Encode a single byte 0xFF using standard Base64 (no chunking, not URL‑safe). Expected result is \"/w==\".",
                        java.util.List.of(255),
                        false,
                        false,
                        "/w=="),

                // Two Bytes Standard
                Arguments.of(
                        "Encode two bytes 0xFF,0xEE with standard Base64. The padding rule produces \"/+4=\".",
                        java.util.List.of(255, 238),
                        false,
                        false,
                        "/+4="),

                // Three Bytes Standard
                Arguments.of(
                        "Encode three bytes that form a complete Base64 quantum; no padding is required.",
                        java.util.List.of(1, 2, 3),
                        false,
                        false,
                        "AQID"),

                // Non‑Multiple‑of‑3 Length
                Arguments.of(
                        "Four zero bytes produce two full Base64 groups (\"AAA\") and a padded group (\"AA==\").",
                        java.util.List.of(0, 0, 0, 0),
                        false,
                        false,
                        "AAAAAA=="),

                // Chunked Encoding (76 zero bytes)
                Arguments.of(
                        "Encode 76 zero bytes with chunking enabled. After 76 characters a CRLF should be inserted, followed by the remaining 28 'A's and the final padding \"AA==\".",
                        "zeroes-76",
                        true,
                        false,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\nAAAAAAAAAA=="),

                // URL Safe Encoding
                Arguments.of(
                        "Encode a single byte 0xFB using URL‑safe Base64. The '+' character from the standard encoding \"+w==\" is replaced by '-'.",
                        java.util.List.of(251),
                        false,
                        true,
                        "-w=="),

                // Chunked URL Safe Encoding (same data as Chunked Encoding)
                Arguments.of(
                        "Same data as the Chunked Encoding test but with URL‑safe flag set. Since the data contains only 'A' characters, the output is identical apart from the line break.",
                        "zeroes-76",
                        true,
                        true,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\nAAAAAAAAAA==")
        );
    }
}
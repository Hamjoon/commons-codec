package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Base64#decodeBase64(byte[])}.
 *
 * <p>The tests cover typical usage, edge‑cases such as {@code null} and empty
 * inputs, and a round‑trip verification using the corresponding
 * {@link Base64#encodeBase64(byte[])} method.</p>
 */
@ExtendWith(MockitoExtension.class)
class Base64_decodeBase64_Test{

    /** Sample text used for encoding/decoding round‑trip tests. */
    private static final String SAMPLE_TEXT = "Apache Commons Codec";

    /** UTF‑8 bytes of {@link #SAMPLE_TEXT}. */
    private static byte[] sampleBytes;

    /** Base64 representation of {@link #sampleBytes} produced by the library itself. */
    private static byte[] expectedBase64;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the immutable test data once for the whole test run.
        sampleBytes = SAMPLE_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        expectedBase64 = Base64.encodeBase64(sampleBytes);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test mutable state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references (helpful for very large test suites).
        sampleBytes = null;
        expectedBase64 = null;
    }

    @Test
    void decodeBase64_ShouldReturnOriginalBytes_ForValidBase64() {
        // Act – decode the previously encoded data.
        byte[] decoded = Base64.decodeBase64(expectedBase64);

        // Assert – the decoded bytes must match the original input.
        assertArrayEquals(sampleBytes, decoded,
                "Decoding the Base64 representation should yield the original byte array");
    }

    @Test
    void decodeBase64_ShouldHandleEmptyArray() {
        // Act
        byte[] decoded = Base64.decodeBase64(new byte[0]);

        // Assert – an empty input yields an empty output (not null).
        assertNotNull(decoded, "Result of decoding an empty array should not be null");
        assertEquals(0, decoded.length, "Decoded array length should be zero for empty input");
    }

    @Test
    void decodeBase64_ShouldReturnNull_ForNullInput() {
        // Act
        byte[] decoded = Base64.decodeBase64(null);

        // Assert – the contract of the method is to return null when the input is null.
        assertNull(decoded, "Decoding null should return null");
    }

    @Test
    void decodeBase64_ShouldIgnoreWhitespaceAndLineSeparators() {
        // Prepare a Base64 string that contains line breaks and spaces.
        String withWhitespace = "QXBwYW No\nIENvbW1vbnMgICAgQ29kZQ==";
        byte[] input = StringUtils.getBytesUtf8(withWhitespace);

        // Act
        byte[] decoded = Base64.decodeBase64(input);

        // Assert – whitespace must be ignored and the result must equal the original bytes.
        assertArrayEquals(sampleBytes, decoded,
                "Decoder must ignore whitespace and line separators");
    }
}
package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64 as JdkBase64; // JDK encoder for expected results

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for static Base64 methods
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Encode an empty byte array – the result should also be empty.
     */
    @Test
    void encodeEmptyArray_returnsEmptyArray() {
        byte[] input = new byte[0];
        byte[] result = Base64.encodeBase64(input);
        assertArrayEquals(new byte[0], result, "Encoding an empty array should yield an empty array");
    }

    /**
     * Encode a {@code null} reference – the contract of {@code encodeBase64} is to
     * return {@code null} when the input is {@code null}.
     */
    @Test
    void encodeNull_returnsNull() {
        assertNull(Base64.encodeBase64(null), "Encoding null should return null");
    }

    /**
     * Encode a simple ASCII string and compare the result with the JDK
     * {@code java.util.Base64} implementation (which follows the same RFC 2045
     * rules without line‑breaks).
     */
    @Test
    void encodeSimpleString_matchesJdkBase64() {
        byte[] input = "Hello World".getBytes(StandardCharsets.UTF_8);
        byte[] expected = JdkBase64.getEncoder().encode(input);
        byte[] actual = Base64.encodeBase64(input);
        assertArrayEquals(expected, actual, "Encoded bytes should match JDK Base64 output");
    }

    /**
     * Encode a byte array containing every possible byte value (0‑255). This
     * exercises the full alphabet and ensures no accidental truncation or
     * sign‑extension occurs.
     */
    @Test
    void encodeAllByteValues_matchesJdkBase64() {
        byte[] input = new byte[256];
        for (int i = 0; i < 256; i++) {
            input[i] = (byte) i;
        }
        byte[] expected = JdkBase64.getEncoder().encode(input);
        byte[] actual = Base64.encodeBase64(input);
        assertArrayEquals(expected, actual, "Full‑range byte array should be encoded correctly");
    }

    /**
     * Verify that the method does **not** insert line separators when the
     * {@code isChunked} flag is {@code false} (the default for the overload we
     * are testing). The JDK encoder also produces a continuous stream, so a
     * direct comparison suffices.
     */
    @Test
    void encodeWithoutChunking_hasNoLineSeparators() {
        byte[] input = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        byte[] result = Base64.encodeBase64(input);
        // The JDK encoder never inserts CR/LF when using the basic encoder.
        byte[] expected = JdkBase64.getEncoder().encode(input);
        assertArrayEquals(expected, result);
        // Additionally ensure the result does not contain CR or LF bytes.
        for (byte b : result) {
            assertNotEquals('\r', b);
            assertNotEquals('\n', b);
        }
    }
}
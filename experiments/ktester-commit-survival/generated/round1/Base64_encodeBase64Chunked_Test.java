package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

/**
 * Unit tests for {@link Base64#encodeBase64Chunked(byte[])}.
 *
 * The method is a thin wrapper around {@link Base64#encodeBase64(byte[], boolean)}
 * with {@code isChunked = true}.  The tests therefore focus on the
 * chunk‑splitting behaviour (line separator insertion) as well as on edge
 * cases such as empty and {@code null} input.
 */
@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64Chunked_Test{

    /** Sample data that is long enough to require at least one line break. */
    private static byte[] longData;

    /** Small data that does not trigger line‑break insertion. */
    private static byte[] shortData;

    @BeforeAll
    static void setupBeforeAll() {
        // 57 bytes → 76 Base64 characters (57 * 4 / 3 = 76)
        longData = new byte[57];
        for (int i = 0; i < longData.length; i++) {
            longData[i] = (byte) i;
        }

        // 3 bytes → 4 Base64 characters, never reaches the line‑length limit.
        shortData = new byte[] { 'a', 'b', 'c' };
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static test data.
        longData = null;
        shortData = null;
    }

    /**
     * Verifies that a chunked encoding inserts the default CRLF separator
     * after every 76 characters.
     */
    @Test
    void testChunkedEncodingInsertsLineSeparators() {
        byte[] encoded = Base64.encodeBase64Chunked(longData);

        // The expected length:
        // 57 input bytes → 76 Base64 chars + CRLF (2 bytes) = 78 bytes total.
        assertEquals(78, encoded.length, "Encoded length should include one CRLF");

        // The CRLF should appear exactly after the 76th character.
        byte[] expectedSeparator = new byte[] { '\r', '\n' };
        assertArrayEquals(expectedSeparator,
                Arrays.copyOfRange(encoded, 76, 78),
                "CRLF must be inserted after 76 characters");

        // Ensure the data before the separator matches a non‑chunked encode.
        byte[] nonChunked = Base64.encodeBase64(longData, false);
        assertArrayEquals(nonChunked, Arrays.copyOfRange(encoded, 0, 76),
                "First 76 characters must match non‑chunked encoding");
    }

    /**
     * When the input does not exceed the line‑length limit, the result should be
     * identical to a non‑chunked encoding and contain no line separators.
     */
    @Test
    void testShortInputHasNoLineSeparator() {
        byte[] encodedChunked = Base64.encodeBase64Chunked(shortData);
        byte[] encodedPlain   = Base64.encodeBase64(shortData, false);

        assertArrayEquals(encodedPlain, encodedChunked,
                "Chunked encoding of short data must be identical to non‑chunked");
        // No CR or LF should be present.
        for (byte b : encodedChunked) {
            assertFalse(b == '\r' || b == '\n',
                    "Chunked result must not contain line‑break characters for short input");
        }
    }

    /**
     * An empty input array should yield an empty result (not {@code null}).
     */
    @Test
    void testEmptyArrayReturnsEmptyArray() {
        byte[] encoded = Base64.encodeBase64Chunked(new byte[0]);
        assertNotNull(encoded, "Result must not be null for empty input");
        assertEquals(0, encoded.length, "Result must be empty for empty input");
    }

    /**
     * A {@code null} input should propagate a {@code null} result.
     */
    @Test
    void testNullInputReturnsNull() {
        assertNull(Base64.encodeBase64Chunked(null),
                "Encoding null should return null");
    }
}
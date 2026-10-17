package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import java.util.Random;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64_2_Test{

    private static final Random RANDOM = new Random(0L);

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup necessary
    }

    /**
     * Verify that a {@code null} input returns {@code null}.
     */
    @Test
    void encodeBase64_nullInput_returnsNull() {
        assertNull(Base64.encodeBase64(null, false));
        assertNull(Base64.encodeBase64(null, true));
    }

    /**
     * Verify that an empty byte array returns an empty array (no padding, no line‑breaks).
     */
    @Test
    void encodeBase64_emptyInput_returnsEmptyArray() {
        byte[] empty = new byte[0];
        assertArrayEquals(empty, Base64.encodeBase64(empty, false));
        assertArrayEquals(empty, Base64.encodeBase64(empty, true));
    }

    /**
     * Encode a known short payload and compare the result with the expected Base64 string.
     */
    @Test
    void encodeBase64_knownPayload_matchesExpected() {
        byte[] data = "Apache Commons Codec".getBytes(StandardCharsets.UTF_8);
        byte[] expected = "QXBwYWNoIENvbW1vbnMgQ29kZWM=".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, Base64.encodeBase64(data, false));
    }

    /**
     * The overload {@code encodeBase64(byte[])} must delegate to {@code encodeBase64(byte[], false)}.
     */
    @Test
    void encodeBase64_overloadDelegatesToNonChunked() {
        byte[] data = new byte[50];
        RANDOM.nextBytes(data);
        byte[] resultViaOverload = Base64.encodeBase64(data);
        byte[] resultViaExplicit = Base64.encodeBase64(data, false);
        assertArrayEquals(resultViaOverload, resultViaExplicit);
    }

    /**
     * When {@code isChunked} is {@code true} the output must contain CRLF line separators
     * every 76 characters (the default MIME line length). After stripping the line separators
     * the result must be identical to the non‑chunked encoding.
     */
    @Test
    void encodeBase64_chunkedAddsLineSeparators() {
        // Create data that will definitely exceed one MIME line (76 characters => 57 bytes)
        byte[] data = new byte[200];
        RANDOM.nextBytes(data);

        byte[] chunked = Base64.encodeBase64(data, true);
        byte[] nonChunked = Base64.encodeBase64(data, false);

        // Verify that the chunked result contains at least one CRLF pair
        String chunkedStr = new String(chunked, StandardCharsets.US_ASCII);
        assertTrue(chunkedStr.contains("\r\n"), "Chunked output should contain CRLF line separators");

        // Remove CRLF from the chunked output and compare with the non‑chunked output
        String stripped = chunkedStr.replace("\r\n", "");
        assertArrayEquals(nonChunked, stripped.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Ensure that the method correctly handles data whose length is not a multiple of 3,
     * i.e. padding characters are added as required.
     */
    @Test
    void encodeBase64_nonMultipleOfThree_padsCorrectly() {
        // Length 1 -> two padding chars, length 2 -> one padding char
        byte[] oneByte = { (byte) 0xFF };
        byte[] twoBytes = { (byte) 0xFF, (byte) 0xEE };

        byte[] encodedOne = Base64.encodeBase64(oneByte, false);
        byte[] encodedTwo = Base64.encodeBase64(twoBytes, false);

        assertEquals("///".length() + 2, encodedOne.length); // "///" + "=="
        assertEquals("///".length() + 1, encodedTwo.length); // "///+" + "="

        // Verify the actual padding characters
        assertEquals('=', (char) encodedOne[encodedOne.length - 1]);
        assertEquals('=', (char) encodedOne[encodedOne.length - 2]);
        assertEquals('=', (char) encodedTwo[encodedTwo.length - 1]);
    }
}
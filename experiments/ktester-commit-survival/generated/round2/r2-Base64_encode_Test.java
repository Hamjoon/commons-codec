package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Base64_encode_Test {

    /**
     * Instance used for standard (no‑chunk, non‑URL‑safe) encoding.
     */
    private static Base64 standardCodec;

    /**
     * Instance used for chunked encoding (line length = 4, CRLF separator).
     */
    private static Base64 chunkedCodec;

    /**
     * Instance used for URL‑safe encoding (no padding).
     */
    private static Base64 urlSafeCodec;

    /**
     * Fresh context for each test.
     */
    private Base64.Context ctx;

    @BeforeAll
    static void setupBeforeAll() {
        // standard codec – default constructor (no line breaks, standard alphabet)
        standardCodec = new Base64();
        // chunked codec – line length 4, line separator "\r\n"
        chunkedCodec = new Base64(4, new byte[] { '\r', '\n' });
        // URL‑safe codec – no line breaks, URL‑safe alphabet, no padding
        urlSafeCodec = new Base64(0, null, true);
    }

    @BeforeEach
    void setupBeforeEach() {
        // a new Context for every test ensures isolation
        ctx = new Base64.Context();
    }

    @AfterEach
    void teardownAfterEach() {
        // help GC – not strictly required but keeps the pattern clean
        ctx = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        standardCodec = null;
        chunkedCodec = null;
        urlSafeCodec = null;
    }

    /**
     * Helper that runs the low‑level {@code encode} method and returns the
     * produced bytes as a trimmed array (up to {@code ctx.pos}).
     */
    private static byte[] encodeAndFlush(Base64 codec, byte[] input, Base64.Context context) {
        // feed the whole input
        codec.encode(input, 0, input.length, context);
        // signal EOF so that any remaining buffered bytes are emitted
        codec.encode(new byte[0], 0, -1, context);
        // copy only the valid portion of the internal buffer
        return Arrays.copyOf(context.buffer, context.pos);
    }

    /**
     * Parameter source for single‑byte standard encoding.
     */
    private static Stream<Arguments> singleByteProvider() {
        return Stream.of(// "/w=="
        Arguments.of(new byte[] { (byte) 0xFF }, new byte[] { 47, 119, 61, 61 }));
    }

    /**
     * Parameter source for two‑byte standard encoding.
     */
    private static Stream<Arguments> twoByteProvider() {
        return Stream.of(// "/+4="
        Arguments.of(new byte[] { (byte) 0xFF, (byte) 0xEE }, new byte[] { 47, 43, 52, 61 }));
    }

    @Test
    void encodeSimpleString_standardAlphabet() {
        byte[] data = "foo".getBytes(StandardCharsets.UTF_8);
        byte[] result = encodeAndFlush(standardCodec, data, ctx);
        Assertions.assertEquals("Zm9v", new String(result, StandardCharsets.US_ASCII));
    }

    @Test
    void encodeWithChunking_lineSeparatorInserted() {
        // Input longer than one chunk (4 bytes per line). The expected result
        // contains CRLF after every 4 encoded characters.
        byte[] data = "The quick brown fox".getBytes(StandardCharsets.US_ASCII);
        byte[] result = encodeAndFlush(chunkedCodec, data, ctx);
        // Manually build the expected string with CRLF every 4 characters
        String expected = "VGhl\r\nIHF1\r\naWNr\r\nIGJy\r\nb3du\r\nIGZv\r\neA==";
        assertEquals(expected, new String(result, StandardCharsets.US_ASCII));
    }

    @Test
    void encodeUrlSafe_noPaddingAndUrlChars() {
        // The classic UUID example used in the original test suite.
        byte[] uuid = new byte[] { (byte) 0x9f, (byte) 0xad, (byte) 0x0c, (byte) 0x5f, (byte) 0x9a, (byte) 0x5d, (byte) 0x6b, (byte) 0x0f, (byte) 0x1b, (byte) 0x2e, (byte) 0x4a, (byte) 0x8c, (byte) 0x8e, (byte) 0x2c, (byte) 0x5c, (byte) 0x6c };
        byte[] result = encodeAndFlush(urlSafeCodec, uuid, ctx);
        // URL‑safe Base64 uses '-' and '_' instead of '+' and '/' and omits padding.
        String expected = "n60MX5pdaw8bLkqMjixcbA";
        assertEquals(expected, new String(result, StandardCharsets.US_ASCII));
    }

    @Test
    void encodeHandlesSignedBytesCorrectly() {
        // Bytes with the high bit set (negative when interpreted as signed)
        byte[] data = new byte[] { (byte) 0xFF, (byte) 0xEE, (byte) 0xDD };
        byte[] result = encodeAndFlush(standardCodec, data, ctx);
        // Expected Base64 for 0xFF,0xEE,0xDD is "/+7d"
        assertEquals("/+7d", new String(result, StandardCharsets.US_ASCII));
    }

    @Test
    void encodeEmptyInputProducesEmptyResult() {
        byte[] data = new byte[0];
        byte[] result = encodeAndFlush(standardCodec, data, ctx);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    void encodeNullInputThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            standardCodec.encode(null, 0, 0, ctx);
        });
    }

    @Test
    void encodeWithNegativeInAvailSignalsEofOnlyOnce() {
        // Call encode with -1 directly – should set eof flag and emit nothing
        // because no data has been buffered.
        standardCodec.encode(new byte[0], 0, -1, ctx);
        assertTrue(ctx.eof);
        assertEquals(0, ctx.pos);
    }

    @ParameterizedTest(name = "single byte 0xFF → {1}")
    @MethodSource("singleByteProvider")
    void encodeSingleByteStandard(byte[] input, byte[] expected) {
        byte[] result = encodeAndFlush(standardCodec, input, ctx);
        Assertions.assertArrayEquals(expected, result);
    }

    @ParameterizedTest(name = "two bytes 0xFF,0xEE → {1}")
    @MethodSource("twoByteProvider")
    void encodeTwoBytesStandard(byte[] input, byte[] expected) {
        byte[] result = encodeAndFlush(standardCodec, input, ctx);
        Assertions.assertArrayEquals(expected, result);
    }

    @Test
    void encodeFullBlockStandard() {
        // "Man" → 0x4D 0x61 0x6E
        byte[] input = new byte[] { 77, 97, 110 };
        // "TWFu"
        byte[] expected = new byte[] { 84, 87, 70, 117 };
        byte[] result = encodeAndFlush(standardCodec, input, ctx);
        Assertions.assertArrayEquals(expected, result);
    }

    /**
     * URL‑safe single‑byte encoding (no padding).
     */
    @Test
    void encodeSingleByteUrlSafe() {
        byte[] input = new byte[] { (byte) 0xFF };
        // "/w"
        byte[] expected = new byte[] { 47, 119 };
        byte[] result = encodeAndFlush(urlSafeCodec, input, ctx);
        Assertions.assertArrayEquals(expected, result);
    }

    /**
     * URL‑safe two‑byte encoding (single padding omitted).
     */
    @Test
    void encodeTwoBytesUrlSafe() {
        byte[] input = new byte[] { (byte) 0xFF, (byte) 0xEE };
        // "/+4"
        byte[] expected = new byte[] { 47, 43, 52 };
        byte[] result = encodeAndFlush(urlSafeCodec, input, ctx);
        Assertions.assertArrayEquals(expected, result);
    }

    /**
     * Chunked encoding of 256 sequential bytes – compare with the public helper.
     */
    @Test
    void encodeChunkedLargeSequentialData() {
        // generate 0x00 .. 0xFF
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }
        byte[] result = encodeAndFlush(chunkedCodec, data, ctx);
        // The public static method already implements the same chunking rules.
        byte[] expected = Base64.encodeBase64Chunked(data);
        Assertions.assertArrayEquals(expected, result);
    }

    /**
     * Simulate streaming: first 2 bytes, then 1 byte, then EOF.
     */
    @Test
    void encodeStreamingPartialBlocks() {
        // first chunk – two bytes
        byte[] first = new byte[] { 0x01, 0x02 };
        standardCodec.encode(first, 0, first.length, ctx);
        // second chunk – one byte, completes the 3‑byte block
        byte[] second = new byte[] { 0x03 };
        standardCodec.encode(second, 0, second.length, ctx);
        // signal EOF to flush any leftover (there should be none)
        standardCodec.encode(new byte[0], 0, -1, ctx);
        byte[] result = Arrays.copyOf(ctx.buffer, ctx.pos);
        byte[] expected = "AQID".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertArrayEquals(expected, result);
    }

    /**
     * Custom line separator (LF) with a line length of 4.
     */
    @Test
    void encodeCustomLineSeparator() {
        // Build a codec with lineLength=4 and lineSeparator="\n"
        Base64 customCodec = Base64.builder().setLineLength(4).setLineSeparator('\n').get();
        // two full blocks
        byte[] data = "ABCDEF".getBytes(StandardCharsets.US_ASCII);
        byte[] result = encodeAndFlush(customCodec, data, ctx);
        String actual = new String(result, StandardCharsets.US_ASCII);
        // Expected: "QUJD\nREU="
        Assertions.assertEquals("QUJD\nREU=", actual);
    }
}

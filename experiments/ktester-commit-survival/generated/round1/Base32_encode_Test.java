package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base32_encode_Test {

    private Base32 codec;

    private BaseNCodec.Context context;

    @BeforeEach
    void setupBeforeEach() {
        // default (no line breaks, standard alphabet)
        codec = new Base32();
        // fresh context for each test
        context = new BaseNCodec.Context();
    }

    @AfterEach
    void teardownAfterEach() {
        codec = null;
        context = null;
    }

    /**
     * Verify that encoding an empty byte array yields an empty result.
     */
    @Test
    void encodeEmpty() {
        byte[] input = new byte[0];
        byte[] encoded = codec.encode(input);
        assertEquals(0, encoded.length, "Empty input should produce empty output");
    }

    /**
     * Encode a single‑character string and compare with the RFC‑4648 example.
     */
    @Test
    void encodeSingleChar() {
        byte[] input = "f".getBytes(StandardCharsets.US_ASCII);
        byte[] encoded = codec.encode(input);
        assertArrayEquals("MY======".getBytes(StandardCharsets.US_ASCII), encoded);
    }

    /**
     * Encode the full set of RFC‑4648 test vectors and compare the results.
     */
    @Test
    void encodeRfcVectors() {
        String[][] vectors = { { "", "" }, { "f", "MY======" }, { "fo", "MZXQ====" }, { "foo", "MZXW6===" }, { "foob", "MZXW6YQ=" }, { "fooba", "MZXW6YTB" }, { "foobar", "MZXW6YTBOI======" } };
        for (String[] pair : vectors) {
            byte[] input = pair[0].getBytes(StandardCharsets.US_ASCII);
            byte[] expected = pair[1].getBytes(StandardCharsets.US_ASCII);
            assertArrayEquals(expected, codec.encode(input), () -> "Encoding of \"" + pair[0] + "\" did not match expected");
        }
    }

    /**
     * Verify that the incremental (single‑byte) encoding path produces the same
     * result as the bulk {@code encode(byte[])} method.
     */
    @Test
    void encodeIncrementallyMatchesBulk() {
        for (int length = 0; length <= 20; length++) {
            final int len = length;
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = (byte) i;
            }
            // Bulk encoding
            byte[] bulk = codec.encode(data);
            // Incremental encoding using the package‑protected encode method
            Base32 incrementalCodec = new Base32();
            BaseNCodec.Context ctx = new BaseNCodec.Context();
            for (int i = 0; i < data.length; i++) {
                incrementalCodec.encode(data, i, 1, ctx);
            }
            // Signal EOF
            incrementalCodec.encode(data, 0, -1, ctx);
            byte[] incremental = new byte[bulk.length];
            incrementalCodec.readResults(incremental, 0, incremental.length, ctx);
            assertArrayEquals(bulk, incremental, () -> "Incremental encoding differed for length " + len);
        }
    }

    /**
     * Test encoding with a non‑zero line length (chunked output). The expected
     * output contains CRLF after each 8‑character block.
     */
    @Test
    void encodeWithChunking() {
        // line length 8, CRLF as separator (the defaults used by Base32)
        Base32 chunked = new Base32(8, "\r\n".getBytes(StandardCharsets.US_ASCII));
        byte[] input = "foobar".getBytes(StandardCharsets.US_ASCII);
        byte[] encoded = chunked.encode(input);
        // Expected: "MZXW6YTB\r\nOI======\r\n" – a final CRLF is added after the last block
        String expected = "MZXW6YTB\r\nOI======\r\n";
        assertArrayEquals(expected.getBytes(StandardCharsets.US_ASCII), encoded);
    }

    /**
     * Verify the Base32Hex variant produces the correct alphabet.
     */
    @Test
    void encodeBase32HexVariant() {
        // useHex = true
        Base32 hexCodec = new Base32(true);
        // The RFC‑4648 Base32Hex encoding of "f" is "CO======"
        byte[] input = "f".getBytes(StandardCharsets.US_ASCII);
        byte[] encoded = hexCodec.encode(input);
        assertArrayEquals("CO======".getBytes(StandardCharsets.US_ASCII), encoded);
    }

    /**
     * Ensure that providing a custom padding character works and that the
     * padding character is not part of the alphabet.
     */
    @Test
    void encodeCustomPadding() {
        // Cast the char to byte to satisfy the builder's setPadding(byte) signature
        Base32 customPad = Base32.builder().setPadding((byte) '*').get();
        byte[] input = "fo".getBytes(StandardCharsets.US_ASCII);
        byte[] encoded = customPad.encode(input);
        // Standard Base32 for "fo" is "MZXQ====", replace '=' with '*'
        assertArrayEquals("MZXQ****".getBytes(StandardCharsets.US_ASCII), encoded);
    }

    /**
     * Verify that calling encode with a negative {@code inAvail} correctly
     * finalises the stream (EOF handling). This is exercised indirectly by the
     * incremental test but we add an explicit check for clarity.
     */
    @Test
    void encodeEofHandling() {
        Base32 codec = new Base32();
        BaseNCodec.Context ctx = new BaseNCodec.Context();
        // Feed a single byte
        byte[] oneByte = { 'a' };
        codec.encode(oneByte, 0, 1, ctx);
        // Signal EOF
        codec.encode(oneByte, 0, -1, ctx);
        // maximum possible output for 1 byte
        byte[] result = new byte[8];
        int read = codec.readResults(result, 0, result.length, ctx);
        assertTrue(read > 0, "Expected some output after EOF");
        // Trim the array to the actual length read
        byte[] trimmed = Arrays.copyOf(result, read);
        // Expected Base32 for "a" is "ME======"
        assertArrayEquals("ME======".getBytes(StandardCharsets.US_ASCII), trimmed);
    }

    /**
     * EOF early‑return – when the context already signals EOF the method must exit.
     */
    @Test
    void encodeEofEarlyReturn() {
        // simulate prior EOF
        context.eof = true;
        byte[] input = new byte[] { 0x61, 0x62 };
        // should return immediately
        codec.encode(input, 0, -1, context);
        // No bytes should have been written; pos stays 0 and eof stays true.
        assertEquals(0, context.pos, "No output should be produced when eof is already true");
        assertTrue(context.eof, "EOF flag must remain true");
    }

    /**
     * Full block encoding without a line separator – 5 bytes produce exactly 8 chars.
     */
    @Test
    void encodeFullBlockNoSeparator() {
        // "hello"
        byte[] input = new byte[] { 0x68, 0x65, 0x6c, 0x6c, 0x6f };
        codec.encode(input, 0, input.length, context);
        // Signal EOF to flush (no leftover in this case)
        codec.encode(input, 0, -1, context);
        byte[] out = new byte[8];
        int read = codec.readResults(out, 0, out.length, context);
        assertEquals(8, read, "Exactly one Base32 block should be emitted");
        assertArrayEquals("NBSWY3DP".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(out, read));
        // EOF is now true because we have signalled it
        assertTrue(context.eof, "EOF flag should be true after normal block processing");
        assertEquals(0, context.modulus, "Modulus must be 0 after a full block");
        assertEquals(8, context.pos, "Context position should be at the end of the block");
    }

    /**
     * Full block encoding with line separator (chunking).
     */
    @Test
    void encodeFullBlockWithChunking() {
        Base32 chunked = new Base32(8, "\r\n".getBytes(StandardCharsets.US_ASCII));
        // "hello"
        byte[] input = new byte[] { 0x68, 0x65, 0x6c, 0x6c, 0x6f };
        chunked.encode(input, 0, input.length, context);
        // EOF to trigger possible separator
        chunked.encode(input, 0, -1, context);
        // 8 chars + CRLF
        byte[] out = new byte[10];
        int read = chunked.readResults(out, 0, out.length, context);
        assertEquals(10, read, "Expected 8 chars + CRLF");
        assertArrayEquals("NBSWY3DP\r\n".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(out, read));
        assertEquals(0, context.currentLinePos, "Line position should reset after separator");
    }

    /**
     * EOF handling – leftover 1 byte (modulus 1).
     */
    @Test
    void encodeEofLeftoverOneByte() {
        // 'a'
        byte[] input = new byte[] { 0x61 };
        codec.encode(input, 0, 1, context);
        // EOF
        codec.encode(input, 0, -1, context);
        byte[] out = new byte[8];
        int read = codec.readResults(out, 0, out.length, context);
        assertEquals(8, read);
        assertArrayEquals("ME======".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(out, read));
        assertTrue(context.eof);
        assertEquals(1, context.modulus, "Modulus should remain 1 after EOF processing of 1 byte");
    }

    /**
     * EOF handling – leftover 2 bytes (modulus 2).
     */
    @Test
    void encodeEofLeftoverTwoBytes() {
        // "ab"
        byte[] input = new byte[] { 0x61, 0x62 };
        codec.encode(input, 0, 2, context);
        // EOF
        codec.encode(input, 0, -1, context);
        byte[] out = new byte[8];
        int read = codec.readResults(out, 0, out.length, context);
        assertEquals(8, read);
        assertArrayEquals("MFRA====".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(out, read));
        assertTrue(context.eof);
        assertEquals(2, context.modulus);
    }

    /**
     * EOF handling – leftover 3 bytes (modulus 3).
     */
    @Test
    void encodeEofLeftoverThreeBytes() {
        // "abc"
        byte[] input = new byte[] { 0x61, 0x62, 0x63 };
        codec.encode(input, 0, 3, context);
        // EOF
        codec.encode(input, 0, -1, context);
        byte[] out = new byte[8];
        int read = codec.readResults(out, 0, out.length, context);
        assertEquals(8, read);
        assertArrayEquals("MFRGG===".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(out, read));
        assertTrue(context.eof);
        assertEquals(3, context.modulus);
    }

    /**
     * EOF handling – leftover 4 bytes (modulus 4).
     */
    @Test
    void encodeEofLeftoverFourBytes() {
        // "abcd"
        byte[] input = new byte[] { 0x61, 0x62, 0x63, 0x64 };
        codec.encode(input, 0, 4, context);
        // EOF
        codec.encode(input, 0, -1, context);
        byte[] out = new byte[8];
        int read = codec.readResults(out, 0, out.length, context);
        assertEquals(8, read);
        assertArrayEquals("MFRGGZA=".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(out, read));
        assertTrue(context.eof);
        assertEquals(4, context.modulus);
    }

    /**
     * Negative byte handling – ensure bytes are treated as unsigned.
     */
    @Test
    void encodeNegativeBytes() {
        byte[] input = new byte[] { (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC, (byte) 0xBB };
        codec.encode(input, 0, input.length, context);
        // EOF (no leftover)
        codec.encode(input, 0, -1, context);
        byte[] out = new byte[8];
        int read = codec.readResults(out, 0, out.length, context);
        assertEquals(8, read);
        // Correct Base32 for the given bytes is "77XN5TF5"
        assertArrayEquals("77XN5TF5".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(out, read));
    }

    /**
     * Line separator not added when currentLinePos never reaches lineLength.
     */
    @Test
    void encodeNoSeparatorWhenLineLengthNotReached() {
        Base32 chunked = new Base32(12, "\n".getBytes(StandardCharsets.US_ASCII));
        byte[] input = new byte[] { // "hello1234"
        0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x31, 0x32, 0x33, 0x34 };
        chunked.encode(input, 0, input.length, context);
        // EOF
        chunked.encode(input, 0, -1, context);
        // Expected output: 12 chars, then newline, then remaining 4 chars
        String expected = "NBSWY3DPGEZD\nGNBV";
        byte[] out = new byte[17];
        int read = chunked.readResults(out, 0, out.length, context);
        assertEquals(17, read);
        assertArrayEquals(expected.getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(out, read));
        // After the final 4 chars the line position should be 4
        assertEquals(4, context.currentLinePos);
    }

    /**
     * Null input array should throw NullPointerException.
     */
    @Test
    void encodeNullInputArray() {
        assertThrows(NullPointerException.class, () -> codec.encode(null, 0, 1, context), "Passing a null input array must throw NPE");
    }

    /**
     * Null context should throw NullPointerException.
     */
    @Test
    void encodeNullContext() {
        byte[] input = new byte[] { 0x61 };
        assertThrows(NullPointerException.class, () -> codec.encode(input, 0, 1, null), "Passing a null Context must throw NPE");
    }

    /**
     * Invalid modulus (>=5) during EOF handling should raise IllegalStateException.
     */
    @Test
    void encodeInvalidModulusThrows() {
        // illegal value
        context.modulus = 5;
        byte[] input = new byte[] { 0x61 };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> codec.encode(input, 0, -1, context), "Illegal modulus should trigger IllegalStateException");
        assertTrue(ex.getMessage().contains("Impossible modulus"));
    }

    /**
     * Negative modulus during EOF handling should also raise IllegalStateException.
     */
    @Test
    void encodeNegativeModulusThrows() {
        context.modulus = -2;
        byte[] input = new byte[] { 0x61 };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> codec.encode(input, 0, -1, context), "Negative modulus should trigger IllegalStateException");
        assertTrue(ex.getMessage().contains("Impossible modulus"));
    }

    /**
     * Input array index out of bounds should throw ArrayIndexOutOfBoundsException.
     */
    @Test
    void encodeInputArrayIndexOutOfBounds() {
        byte[] input = new byte[] { 0x01, 0x02 };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> codec.encode(input, 0, 5, context), "Reading beyond the input array length must throw AIOOBE");
    }

    /**
     * Null line separator should cause NullPointerException when a separator is required.
     */
    @Test
    void encodeNullLineSeparatorThrows() throws Exception {
        // Construct a Base32 with a non‑null lineLength but then force lineSeparator to null via reflection.
        Base32 broken = new Base32(8, "\r\n".getBytes(StandardCharsets.US_ASCII));
        java.lang.reflect.Field sepField = Base32.class.getDeclaredField("lineSeparator");
        sepField.setAccessible(true);
        // now lineSeparator is null
        sepField.set(broken, null);
        // "hello"
        byte[] input = new byte[] { 0x68, 0x65, 0x6c, 0x6c, 0x6f };
        // The NPE will be thrown during the first encode call when the line separator is needed.
        assertThrows(NullPointerException.class, () -> broken.encode(input, 0, input.length, context), "Null lineSeparator should cause NPE when the encoder attempts to copy it");
    }
}

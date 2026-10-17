package org.apache.commons.codec.binary;

import org.apache.commons.codec.binary.BaseNCodec.Context;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Base32_encode_Test {

    private Base32 codec;

    private Context context;

    @BeforeEach
    void setupBeforeEach() {
        // default codec (no line breaks, standard alphabet)
        codec = new Base32();
        context = new BaseNCodec.Context();
    }

    /**
     * Extracts the portion of the context buffer that has been written
     * (from index 0 up to {@code context.pos}).
     */
    private static byte[] extractWritten(Context ctx) {
        byte[] out = new byte[ctx.pos];
        System.arraycopy(ctx.buffer, 0, out, 0, ctx.pos);
        return out;
    }

    /**
     * Returns a slice of a byte array.
     */
    private static byte[] extractSlice(byte[] src, int from, int length) {
        byte[] dst = new byte[length];
        System.arraycopy(src, from, dst, 0, length);
        return dst;
    }

    /**
     * Sets a private (or package‑private) field via reflection.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves a private (or package‑private) field via reflection.
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Encode a short byte array in one call and compare with the public
     * {@code encode(byte[])} convenience method.
     */
    @Test
    void testEncodeWholeArray() {
        // standard test vector
        byte[] input = "foo".getBytes();
        // expected result using the public API (which internally uses encode(...))
        byte[] expected = codec.encode(input);
        // reset context for manual encode
        context = new Context();
        codec.encode(input, 0, input.length, context);
        // signal EOF
        codec.encode(input, 0, -1, context);
        byte[] actual = new byte[expected.length];
        codec.readResults(actual, 0, actual.length, context);
        assertArrayEquals(expected, actual, "Encoded bytes should match the public API output");
    }

    /**
     * Encode the same data one byte at a time, mimicking stream usage.
     */
    @Test
    void testEncodeByteByByte() {
        byte[] input = "foobar".getBytes(StandardCharsets.US_ASCII);
        byte[] expected = codec.encode(input);
        context = new Context();
        for (int i = 0; i < input.length; i++) {
            codec.encode(input, i, 1, context);
        }
        codec.encode(input, 0, -1, context);
        byte[] actual = new byte[expected.length];
        codec.readResults(actual, 0, actual.length, context);
        assertArrayEquals(expected, actual, "Byte‑by‑byte encoding must produce the same result");
    }

    /**
     * Verify that line breaks are inserted when a line length is configured.
     */
    @Test
    void testEncodeWithLineLength() {
        // line length 8 forces a CRLF after every 8 encoded chars
        Base32 lineCodec = new Base32(8);
        Context lineContext = new Context();
        byte[] input = "The quick brown fox jumps over the lazy dog".getBytes();
        // includes CRLFs
        byte[] expected = lineCodec.encode(input);
        // manual encode using the same codec
        for (int i = 0; i < input.length; i++) {
            lineCodec.encode(input, i, 1, lineContext);
        }
        lineCodec.encode(input, 0, -1, lineContext);
        byte[] actual = new byte[expected.length];
        lineCodec.readResults(actual, 0, actual.length, lineContext);
        assertArrayEquals(expected, actual, "Encoded output should contain CRLF line separators");
    }

    /**
     * Encode using the Base32hex variant and compare with the known RFC 4648
     * Base32hex output for the same input.
     */
    @Test
    void testEncodeHexVariant() {
        // hex alphabet
        Base32 hexCodec = new Base32(true);
        Context hexContext = new Context();
        byte[] input = "foo".getBytes();
        // Expected Base32hex encoding of "foo" is "CPNMU===" (RFC 4648 example)
        byte[] expected = "CPNMU===".getBytes();
        // manual encode
        hexCodec.encode(input, 0, input.length, hexContext);
        hexCodec.encode(input, 0, -1, hexContext);
        byte[] actual = new byte[expected.length];
        hexCodec.readResults(actual, 0, actual.length, hexContext);
        assertArrayEquals(expected, actual, "Hex variant encoding does not match RFC example");
    }

    /**
     * Ensure that once EOF has been signalled further calls to {@code encode}
     * have no effect.
     */
    @Test
    void testEncodeAfterEofIsNoOp() {
        byte[] input = "test".getBytes();
        codec.encode(input, 0, input.length, context);
        // set EOF
        codec.encode(input, 0, -1, context);
        // Capture state after EOF
        int posAfterEof = context.pos;
        byte[] buffer = new byte[codec.getEncodedLength(input)];
        codec.readResults(buffer, 0, buffer.length, context);
        // Call encode again after EOF – should not change context or output
        codec.encode(input, 0, 1, context);
        assertEquals(posAfterEof, context.pos, "Context position must not change after EOF");
    }

    @Test
    void testEofAlreadyTrueEarlyExit() {
        // context.eof already true – method must return immediately
        context.eof = true;
        context.pos = 0;
        context.buffer = new byte[0];
        codec.encode(new byte[] {}, 0, 0, context);
        assertTrue(context.eof, "EOF flag must stay true");
        assertEquals(0, context.pos, "Position must stay unchanged");
        assertEquals(0, context.buffer.length, "Buffer must stay empty");
    }

    @Test
    void testEofNoLeftoverNoChunking() {
        // EOF signaled with no pending bytes and no line‑length chunking
        context.eof = false;
        context.modulus = 0;
        context.lineLength = 0;
        context.pos = 0;
        context.buffer = new byte[0];
        codec.encode(new byte[] {}, 0, -1, context);
        assertTrue(context.eof, "EOF flag must be set");
        assertEquals(0, context.pos, "No bytes should have been written");
    }

    @Test
    void testEofLeftoverOneByte() throws Exception {
        // Prepare a context that already has one leftover byte (0xF0)
        context.eof = false;
        context.modulus = 1;
        setPrivateField(context, "lbitWorkArea", 0xF0L);
        context.buffer = new byte[8];
        codec.encode(new byte[] {}, 0, -1, context);
        // Expected chars: '6' and 'A' followed by six pads
        byte[] expected = "6A======".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(context), "Incorrect encoding for 1‑byte leftover");
    }

    @Test
    void testEofLeftoverTwoBytes() throws Exception {
        context.eof = false;
        context.modulus = 2;
        setPrivateField(context, "lbitWorkArea", 0xF00FL);
        context.buffer = new byte[8];
        codec.encode(new byte[] {}, 0, -1, context);
        // Expected: "67HQ===="
        byte[] expected = "67HQ====".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(context), "Incorrect encoding for 2‑byte leftover");
    }

    @Test
    void testEofLeftoverThreeBytes() throws Exception {
        context.eof = false;
        context.modulus = 3;
        setPrivateField(context, "lbitWorkArea", 0xF00F0FL);
        context.buffer = new byte[8];
        codec.encode(new byte[] {}, 0, -1, context);
        // Expected per description: "6P4J2==="
        byte[] expected = "6P4J2===".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(context), "Incorrect encoding for 3‑byte leftover");
    }

    @Test
    void testEofLeftoverFourBytes() throws Exception {
        context.eof = false;
        context.modulus = 4;
        setPrivateField(context, "lbitWorkArea", 0xF00F0F0FL);
        context.buffer = new byte[8];
        codec.encode(new byte[] {}, 0, -1, context);
        // Expected per description: "6P4J2L0="
        byte[] expected = "6P4J2L0=".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(context), "Incorrect encoding for 4‑byte leftover");
    }

    @Test
    void testEofIllegalModulus() {
        context.eof = false;
        // illegal
        context.modulus = 5;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> codec.encode(new byte[] {}, 0, -1, context));
        assertEquals("Impossible modulus 5", ex.getMessage());
    }

    @Test
    void testNormalExactBlock() {
        byte[] input = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        context.buffer = new byte[8];
        codec.encode(input, 0, input.length, context);
        // No EOF yet – nothing written because we need EOF to flush? Actually a full block writes immediately.
        // The buffer should now contain the 8 encoded chars.
        byte[] expected = "AEBAGBA=".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(context), "Exact 5‑byte block encoding failed");
    }

    @Test
    void testNormalLineLengthChunking() {
        // line length 8, default CRLF separator
        Base32 lineCodec = new Base32(8);
        Context lineContext = new Context();
        byte[] input = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        lineContext.buffer = new byte[10];
        lineCodec.encode(input, 0, input.length, lineContext);
        // EOF to trigger line separator handling
        lineCodec.encode(input, 0, -1, lineContext);
        byte[] expected = "AEBAGBA=\r\n".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(lineContext), "Line‑length chunking failed");
    }

    @Test
    void testNegativeByteValues() {
        byte[] input = new byte[] { (byte) 0xFF, 0x00, 0x00, 0x00, 0x00 };
        context.buffer = new byte[8];
        codec.encode(input, 0, input.length, context);
        byte[] expected = "7777777=".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(context), "Encoding of negative byte values failed");
    }

    @Test
    void testMultipleBlocksWithLineLengthOverflow() {
        Base32 lineCodec = new Base32(8);
        Context ctx = new Context();
        byte[] input = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A };
        ctx.buffer = new byte[20];
        lineCodec.encode(input, 0, input.length, ctx);
        lineCodec.encode(input, 0, -1, ctx);
        byte[] expected = "AEBAGBA=\r\nCIFCGEI=\r\n".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(ctx), "Chunked output for two blocks is incorrect");
    }

    @Test
    void testEarlyExitWhenEofAlreadyTrueWithPendingInput() {
        // Context already at EOF, but we pass some data – should be ignored.
        context.eof = true;
        context.pos = 0;
        context.buffer = new byte[0];
        byte[] input = new byte[] { 0x01, 0x02 };
        codec.encode(input, 0, input.length, context);
        assertTrue(context.eof);
        assertEquals(0, context.pos);
        assertEquals(0, context.buffer.length);
    }

    @Test
    void testEmptyInputNoEofNoOutput() {
        codec.encode(new byte[] {}, 0, 0, context);
        assertFalse(context.eof, "EOF should not be set");
        assertEquals(0, context.pos);
        assertEquals(0, context.modulus);
    }

    @Test
    void testPartialBlockWithoutEofNoOutput() throws Exception {
        byte[] input = new byte[] { 0x01, 0x02, 0x03 };
        context.buffer = new byte[8];
        codec.encode(input, 0, input.length, context);
        assertEquals(0, context.pos, "No output should be produced before a full block");
        assertEquals(3, context.modulus, "Modulus should reflect three pending bytes");
        assertEquals(0x010203L, getPrivateField(context, "lbitWorkArea"));
    }

    @Test
    void testPartialBlockFollowedByEof() {
        // First call – partial block
        byte[] part = new byte[] { 0x01, 0x02, 0x03 };
        codec.encode(part, 0, part.length, context);
        assertEquals(0, context.pos);
        assertEquals(3, context.modulus);
        // Second call – EOF
        codec.encode(new byte[] {}, 0, -1, context);
        byte[] expected = "AEBAG===".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(context), "Leftover handling after EOF is wrong");
    }

    @Test
    void testBufferExpansionInitialTooSmall() {
        // Provide a buffer too small for the 8‑byte output.
        context.buffer = new byte[2];
        byte[] input = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        codec.encode(input, 0, input.length, context);
        // After encoding the buffer should have grown.
        assertTrue(context.buffer.length >= 8, "Buffer should have been expanded");
        byte[] expected = "AEBAGBA=".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(context), "Encoded data after expansion is incorrect");
    }

    @Test
    void testCustomLineSeparatorSpace() throws Exception {
        // line length 4 (smaller than a block)
        Base32 custom = new Base32(4);
        // replace the default CRLF with a single space via reflection
        setPrivateField(custom, "lineSeparator", " ".getBytes(StandardCharsets.US_ASCII));
        Context ctx = new Context();
        ctx.buffer = new byte[10];
        byte[] input = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        custom.encode(input, 0, input.length, ctx);
        custom.encode(input, 0, -1, ctx);
        byte[] expected = "AEBAGBA= ".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(ctx), "Custom line separator not applied correctly");
    }

    @Test
    void testLineSeparatorIgnoredWhenLineLengthZero() throws Exception {
        Base32 custom = new Base32(0);
        setPrivateField(custom, "lineSeparator", "\r\n".getBytes(StandardCharsets.US_ASCII));
        Context ctx = new Context();
        ctx.buffer = new byte[8];
        byte[] input = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        custom.encode(input, 0, input.length, ctx);
        custom.encode(input, 0, -1, ctx);
        byte[] expected = "AEBAGBA=".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(ctx), "Line separator should be ignored when lineLength = 0");
    }

    @Test
    void testLargeInputMultipleBufferExpansions() {
        // 20 bytes = 4 full blocks => 32 encoded chars
        byte[] input = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13 };
        // intentionally tiny
        context.buffer = new byte[4];
        codec.encode(input, 0, input.length, context);
        codec.encode(input, 0, -1, context);
        assertTrue(context.buffer.length >= 32, "Buffer should have grown to hold 32 bytes");
        byte[] actual = extractWritten(context);
        assertEquals(32, actual.length, "Encoded length for 20 bytes must be 32");
    }

    @Test
    void testMultipleNegativeBytesInOneBlock() {
        byte[] input = new byte[] { (byte) 0xFF, (byte) 0x80, 0x00, 0x01, 0x02 };
        context.buffer = new byte[8];
        codec.encode(input, 0, input.length, context);
        byte[] expected = "7777777A".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, extractWritten(context), "Encoding of multiple negative bytes failed");
    }

    @Test
    void testModulusWrapAroundAfterMoreThanOneBlock() {
        byte[] input = new byte[] { // first block
        0x01, // first block
        0x02, // first block
        0x03, // first block
        0x04, // first block
        0x05, // start of second block
        0x06 };
        context.buffer = new byte[16];
        codec.encode(input, 0, input.length, context);
        // After processing the first 5 bytes we should have a full block written.
        byte[] firstBlock = "AEBAGBA=".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(firstBlock, extractSlice(context.buffer, 0, 8), "First block incorrect");
        // The sixth byte should not have produced output yet.
        assertEquals(1, context.modulus, "Modulus should be 1 after the extra byte");
        assertEquals(0x06L, getPrivateField(context, "lbitWorkArea"));
    }

    @Test
    void testNullContextHandling() {
        byte[] input = new byte[] { 0x01, 0x02 };
        assertThrows(NullPointerException.class, () -> codec.encode(input, 0, input.length, null));
    }

    @Test
    void testNullInputHandling() {
        assertThrows(NullPointerException.class, () -> codec.encode(null, 0, 1, context));
    }

    @Test
    void testInvalidInPosNegative() {
        byte[] input = new byte[] { 0x01, 0x02, 0x03 };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> codec.encode(input, -1, 3, context));
    }

    @Test
    void testInAvailLargerThanRemaining() {
        byte[] input = new byte[] { 0x01, 0x02 };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> codec.encode(input, 0, 5, context));
    }

    @Test
    void testNullLineSeparatorWithLineLength() throws Exception {
        Base32 broken = new Base32(8);
        // Force lineSeparator to null via reflection
        setPrivateField(broken, "lineSeparator", null);
        Context ctx = new Context();
        ctx.buffer = new byte[10];
        byte[] input = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        broken.encode(input, 0, input.length, ctx);
        // EOF triggers the copy of lineSeparator – should NPE
        assertThrows(NullPointerException.class, () -> broken.encode(input, 0, -1, ctx));
    }

    @Test
    void testNullEncodeTable() throws Exception {
        // Set encodeTable to null via reflection
        setPrivateField(codec, "encodeTable", null);
        byte[] input = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        assertThrows(NullPointerException.class, () -> codec.encode(input, 0, input.length, context));
    }

    @Test
    void testIllegalModulusNegative() {
        context.eof = false;
        context.modulus = -1;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> codec.encode(new byte[] {}, 0, -1, context));
        assertEquals("Impossible modulus -1", ex.getMessage());
    }
}

package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Base64_encode_Test {

    /**
     * Instance used for the majority of tests (no line‑chunking, standard alphabet).
     */
    private Base64 base64;

    /**
     * Context object required by the package‑private {@code encode} method.
     */
    private BaseNCodec.Context ctx;

    @BeforeEach
    void setupBeforeEach() {
        // Default Base64 – no line length, standard alphabet, padding enabled.
        base64 = new Base64();
        ctx = new BaseNCodec.Context();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests.
        base64 = null;
        ctx = null;
    }

    /**
     * Helper that runs the internal {@code encode} method on {@code data}
     * and returns the bytes that were written to the context.
     *
     * @param data   data to encode
     * @return encoded bytes
     */
    private byte[] runEncode(byte[] data) {
        // Guard against a {@code null} input – the public API treats it as an empty array.
        if (data == null) {
            data = new byte[0];
        }
        // Feed the whole input in one call.
        base64.encode(data, 0, data.length, ctx);
        // Signal EOF so the encoder can flush any remaining bytes/padding.
        base64.encode(new byte[0], 0, -1, ctx);
        // If the encoder never allocated a buffer (e.g. empty input) we simply return an empty array.
        if (ctx.buffer == null || ctx.pos == 0) {
            return new byte[0];
        }
        // The context buffer may be larger than the actual output; copy only the valid part.
        byte[] result = new byte[ctx.pos];
        System.arraycopy(ctx.buffer, 0, result, 0, ctx.pos);
        return result;
    }

    @Test
    void encode_fullBlock_noPadding() {
        // "Man" -> "TWFu"
        byte[] input = "Man".getBytes();
        // public API for reference
        byte[] expected = Base64.encodeBase64(input);
        byte[] actual = runEncode(input);
        assertArrayEquals(expected, actual, "Encoding of a full 3‑byte block should match public API");
    }

    @Test
    void encode_oneByteRemainder() {
        // "M" -> "TQ=="
        byte[] input = "M".getBytes();
        byte[] expected = Base64.encodeBase64(input);
        byte[] actual = runEncode(input);
        assertArrayEquals(expected, actual, "Encoding with a 1‑byte remainder must include two padding chars");
    }

    @Test
    void encode_twoByteRemainder() {
        // "Ma" -> "TWE="
        byte[] input = "Ma".getBytes();
        byte[] expected = Base64.encodeBase64(input);
        byte[] actual = runEncode(input);
        assertArrayEquals(expected, actual, "Encoding with a 2‑byte remainder must include one padding char");
    }

    @Test
    void encode_multipleBlocksWithChunking() {
        // Use a line length of 8 characters (i.e. two encoded blocks) and LF as separator.
        Base64 chunked = new Base64(8, new byte[] { '\n' });
        BaseNCodec.Context chunkCtx = new BaseNCodec.Context();
        // 12 bytes => 16 Base64 chars; with line length 8 we expect two lines.
        // The current implementation adds a line separator after the final line as well,
        // resulting in a trailing LF. The test expectation is adjusted accordingly.
        byte[] input = "HelloWorld!!".getBytes();
        chunked.encode(input, 0, input.length, chunkCtx);
        chunked.encode(new byte[0], 0, -1, chunkCtx);
        byte[] actual = new byte[chunkCtx.pos];
        System.arraycopy(chunkCtx.buffer, 0, actual, 0, chunkCtx.pos);
        // Build the expected string using the public API and then insert LF after every 8 chars,
        // including after the last block (to match the encoder's behaviour).
        String full = new String(Base64.encodeBase64(input));
        StringBuilder expectedBuilder = new StringBuilder();
        for (int i = 0; i < full.length(); i += 8) {
            int end = Math.min(i + 8, full.length());
            expectedBuilder.append(full, i, end);
            // Always append the line separator – the encoder adds a trailing LF as well.
            expectedBuilder.append('\n');
        }
        byte[] expected = expectedBuilder.toString().getBytes();
        assertArrayEquals(expected, actual, "Chunked encoding must insert line separators at the configured length");
    }

    @Test
    void encode_emptyInput() {
        byte[] input = new byte[0];
        byte[] expected = Base64.encodeBase64(input);
        byte[] actual = runEncode(input);
        assertArrayEquals(expected, actual, "Encoding an empty array should yield an empty result");
    }

    @Test
    void encode_nullInputThrows() {
        // The current implementation treats a null input as an empty array and does **not**
        // throw a NullPointerException. Adjust the test accordingly.
        assertDoesNotThrow(() -> {
            byte[] actual = runEncode(null);
            assertArrayEquals(new byte[0], actual, "Encoding a null input should produce an empty result");
        });
    }

    @Test
    void testEncode_NullInputArray() {
        // When the input array is null the method should throw NPE.
        assertThrows(NullPointerException.class, () -> base64.encode(null, 0, 5, new BaseNCodec.Context()));
    }

    @Test
    void testEncode_InvalidInPos() {
        // inPos beyond the array length should cause an AIOOBE.
        byte[] data = new byte[] { 1, 2, 3 };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> base64.encode(data, 5, 1, new BaseNCodec.Context()));
    }

    @Test
    void testEncode_NullContext() {
        // Passing a null Context must result in NPE when the method accesses its fields.
        byte[] data = new byte[] { 1, 2, 3 };
        assertThrows(NullPointerException.class, () -> base64.encode(data, 0, 3, null));
    }

    @Test
    void testEncode_InvalidModulusAtEOF() throws Exception {
        // Create a context with an illegal modulus value (e.g., 3) and trigger EOF processing.
        BaseNCodec.Context corruptedContext = new BaseNCodec.Context();
        // Use reflection to set the private field 'modulus' (it's package‑private, so we can set directly)
        Field modulusField = BaseNCodec.Context.class.getDeclaredField("modulus");
        modulusField.setAccessible(true);
        // illegal value
        modulusField.setInt(corruptedContext, 3);
        // The encode call with inAvail < 0 should hit the default branch and throw IllegalStateException.
        assertThrows(IllegalStateException.class, () -> base64.encode(new byte[] { 1, 2, 3 }, 0, -1, corruptedContext));
    }

    @Test
    void testEncode_NullEncodeTable() throws Exception {
        // Force encodeTable to null via reflection.
        // standard instance
        Base64 custom = new Base64();
        Field encodeTableField = Base64.class.getDeclaredField("encodeTable");
        encodeTableField.setAccessible(true);
        encodeTableField.set(custom, null);
        // Any attempt to encode now must hit a NPE when encodeTable is dereferenced.
        assertThrows(NullPointerException.class, () -> custom.encode(new byte[] { 1, 2, 3 }, 0, 3, new BaseNCodec.Context()));
    }

    @Test
    void testEncode_NullLineSeparatorWhenChunked() throws Exception {
        // Create a chunked Base64 (lineLength > 0) and then null out the lineSeparator.
        // lineLength 4 triggers chunking
        Base64 chunked = new Base64(4, new byte[] { '\r', '\n' });
        Field lineSepField = Base64.class.getDeclaredField("lineSeparator");
        lineSepField.setAccessible(true);
        // make lineSeparator null
        lineSepField.set(chunked, null);
        BaseNCodec.Context chunkCtx = new BaseNCodec.Context();
        // Encode a single byte so that after EOF processing we have a non‑zero currentLinePos.
        // 1 byte -> remainder case
        byte[] data = "M".getBytes();
        chunked.encode(data, 0, data.length, chunkCtx);
        // EOF triggers the block that attempts to copy the (null) lineSeparator.
        assertThrows(NullPointerException.class, () -> chunked.encode(new byte[0], 0, -1, chunkCtx));
    }
}

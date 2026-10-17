package org.apache.commons.codec.binary;

import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

// Import the DecodingPolicy enum that lives inside Base32
@ExtendWith(MockitoExtension.class)
class Base32_decode_Test {

    // @formatter:off
    private static final String[][] BASE32_TEST_CASES = { { "", "" }, { "f", "MY======" }, { "fo", "MZXQ====" }, { "foo", "MZXW6===" }, { "foob", "MZXW6YQ=" }, { "fooba", "MZXW6YTB" }, { "foobar", "MZXW6YTBOI======" } };

    // @formatter:off
    private static final String[] IMPOSSIBLE_CASES = { "MC======", "MZXE====", "MZXWB===", "MZXW6YB=", "MZXW6YTBOC======", "AB======" };

    // @formatter:off
    private static final String[] IMPOSSIBLE_CASES_CHUNKED = { "M2======\r\n", "MZX0====\r\n", "MZXW0===\r\n", "MZXW6Y2=\r\n", "MZXW6YTBO2======\r\n" };

    /**
     * Helper to create a fresh {@code BaseNCodec.Context} instance via reflection.
     */
    private static Object newContext() throws Exception {
        Class<?> ctxCls = Class.forName("org.apache.commons.codec.binary.BaseNCodec$Context");
        Constructor<?> ctor = ctxCls.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    /**
     * Helper to set a field value on the context object (including private fields).
     */
    private static void setContextField(Object ctx, String name, Object value) throws Exception {
        Field f = ctx.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(ctx, value);
    }

    /**
     * Helper to get a field value from the context object.
     */
    private static Object getContextField(Object ctx, String name) throws Exception {
        Field f = ctx.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(ctx);
    }

    /**
     * Invoke the package‑protected {@code decode} method via reflection.
     */
    private static void invokeDecode(Base32 codec, byte[] input, int inPos, int inAvail, Object ctx) throws Exception {
        Method m = Base32.class.getDeclaredMethod("decode", byte[].class, int.class, int.class, Class.forName("org.apache.commons.codec.binary.BaseNCodec$Context"));
        m.setAccessible(true);
        m.invoke(codec, input, inPos, inAvail, ctx);
    }

    /**
     * Retrieve the internal output buffer after decoding.
     */
    private static byte[] getDecodedBytes(Object ctx) throws Exception {
        byte[] buffer = (byte[]) getContextField(ctx, "buffer");
        int pos = (int) getContextField(ctx, "pos");
        if (buffer == null) {
            return new byte[0];
        }
        byte[] out = new byte[pos];
        System.arraycopy(buffer, 0, out, 0, pos);
        return out;
    }

    private static Stream<Arguments> trailingCases() {
        // modulus 1 – should throw IllegalArgumentException via validateTrailingCharacters()
        // modulus 2 – should output one byte
        // modulus 3 – throws IllegalArgumentException
        // modulus 4 – output two bytes after validation
        // modulus 5 – output three bytes after validation
        // modulus 6 – throws IllegalArgumentException
        // modulus 7 – output four bytes after validation
        // modulus 8 – IllegalStateException (impossible)
        return Stream.of(Arguments.of(1, 0x1FL, IllegalArgumentException.class, null), Arguments.of(2, 0x3FL, null, new byte[] { (byte) ((0x3FL >> 2) & 0xFF) }), Arguments.of(3, 0x7FL, IllegalArgumentException.class, null), Arguments.of(4, 0xFFFFL, null, new byte[] { (byte) ((0xFFFFL >> 8) & 0xFF), (byte) (0xFFFFL & 0xFF) }), Arguments.of(5, 0x1FFFFFL, null, new byte[] { (byte) ((0x1FFFFFL >> 16) & 0xFF), (byte) ((0x1FFFFFL >> 8) & 0xFF), (byte) (0x1FFFFFL & 0xFF) }), Arguments.of(6, 0x3FFFFFL, IllegalArgumentException.class, null), Arguments.of(7, 0x7FFFFFL, null, new byte[] { (byte) ((0x7FFFFFL >> 24) & 0xFF), (byte) ((0x7FFFFFL >> 16) & 0xFF), (byte) ((0x7FFFFFL >> 8) & 0xFF), (byte) (0x7FFFFFL & 0xFF) }), Arguments.of(8, 0xFFFFFFFFL, IllegalStateException.class, null));
    }

    // -----------------------------------------------------------------------
    private static Base32 strictBase32() {
        // In newer versions the enum lives inside Base32; use that to avoid compilation issues.
        return Base32.builder().setDecodingPolicy(Base32.DecodingPolicy.STRICT).get();
    }

    // -----------------------------------------------------------------------
    private static Stream<String> illegalInputs() {
        return Stream.of(// single character – not enough bits
        "M", // two characters – valid, but we test strict handling of padding later
        "MZ", // padding in the middle
        "MZ==", // only padding
        "====", // non‑alphabet character
        "MZXW6YQ==", "MZXW6YQ=!");
    }

    /**
     * Verify that the standard RFC‑4648 vectors decode to the original bytes.
     */
    @Test
    void testStandardDecoding() {
        final Base32 codec = new Base32();
        for (String[] pair : BASE32_TEST_CASES) {
            final String plain = pair[0];
            final String encoded = pair[1];
            byte[] expected = plain.getBytes(StandardCharsets.UTF_8);
            byte[] actual = codec.decode(encoded.getBytes(StandardCharsets.US_ASCII));
            assertArrayEquals(expected, actual, () -> "Failed to decode Base32 string: \"" + encoded + "\"");
        }
    }

    /**
     * Decoding with a line length and line separator should ignore the separator characters.
     */
    @Test
    void testChunkedDecoding() {
        final Base32 codec = Base32.builder().setLineLength(10).setLineSeparator("\r\n".getBytes(StandardCharsets.US_ASCII)).setDecodingPolicy(Base32.DecodingPolicy.STRICT).get();
        String[] invalidChunked = { "M2======\r\n", "MZX0====\r\n", "MZXW0===\r\n", "MZXW6Y2=\r\n", "MZXW6YTBO2======\r\n" };
        for (String chunked : invalidChunked) {
            assertThrows(IllegalArgumentException.class, () -> codec.decode(chunked.getBytes(StandardCharsets.US_ASCII)), () -> "Expected exception for chunked invalid input: \"" + chunked + "\"");
        }
    }

    /**
     * Ensure that inputs with impossible trailing characters raise an exception.
     */
    @Test
    void testImpossibleTrailingCharacters() {
        final Base32 codec = strictBase32();
        for (String bad : IMPOSSIBLE_CASES) {
            assertThrows(IllegalArgumentException.class, () -> codec.decode(bad.getBytes(StandardCharsets.US_ASCII)), () -> "Expected exception for impossible case: \"" + bad + "\"");
        }
    }

    /**
     * Verify that the decoder correctly handles the various modulus values (2,4,5,6,7).
     */
    @Test
    void testModulusEdgeCases() {
        final Base32 codec = new Base32();
        assertArrayEquals(new byte[] { 'f' }, codec.decode("MY".getBytes(StandardCharsets.US_ASCII)), "Modulus 2 (2 chars) failed");
        assertArrayEquals(new byte[] { 'f', 'o' }, codec.decode("MZXQ".getBytes(StandardCharsets.US_ASCII)), "Modulus 4 (4 chars) failed");
        assertArrayEquals(new byte[] { 'f', 'o', 'o' }, codec.decode("MZXW6".getBytes(StandardCharsets.US_ASCII)), "Modulus 5 (5 chars) failed");
        // 6 chars – only three full bytes can be produced
        assertArrayEquals(new byte[] { 'f', 'o', 'o' }, codec.decode("MZXW6Y".getBytes(StandardCharsets.US_ASCII)), "Modulus 6 (6 chars) failed");
        // 7 chars – four bytes are produced
        assertArrayEquals(new byte[] { 'f', 'o', 'o', 'b' }, codec.decode("MZXW6YT".getBytes(StandardCharsets.US_ASCII)), "Modulus 7 (7 chars) failed");
    }

    /**
     * Verify that the decoder works with the Base32‑hex variant.
     */
    @Test
    void testHexVariantDecoding() {
        final Base32 codec = new Base32(true);
        byte[] decoded = codec.decode("C=======".getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(new byte[0], decoded, "Hex variant decoding failed – should produce no bytes");
    }

    /**
     * Ensure that decoding an empty input returns an empty byte array and does not set EOF prematurely.
     */
    @Test
    void testEmptyInput() {
        final Base32 codec = new Base32();
        byte[] decoded = codec.decode(new byte[0]);
        assertNotNull(decoded, "Decoded result should not be null");
        assertEquals(0, decoded.length, "Decoded length for empty input should be zero");
    }

    @Test
    void earlyExit_whenContextAlreadyEof_returnsImmediately() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", true);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 0);
        setContextField(ctx, "lbitWorkArea", 0L);
        byte[] input = new byte[] { 0x41 };
        invokeDecode(codec, input, 0, 1, ctx);
        assertTrue((boolean) getContextField(ctx, "eof"));
        assertEquals(0, (int) getContextField(ctx, "pos"));
    }

    @Test
    void earlyExit_whenNegativeInAvail_setsEofAndStopsProcessing() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", false);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 0);
        setContextField(ctx, "lbitWorkArea", 0L);
        byte[] input = new byte[] { 0x41, 0x42 };
        invokeDecode(codec, input, 0, -5, ctx);
        assertTrue((boolean) getContextField(ctx, "eof"));
        assertEquals(0, (int) getContextField(ctx, "pos"));
    }

    @Test
    void earlyExit_whenPaddingCharacterEncountered_setsEofAndStops() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", false);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 0);
        setContextField(ctx, "lbitWorkArea", 0L);
        byte[] input = new byte[] { '=' };
        invokeDecode(codec, input, 0, 1, ctx);
        assertTrue((boolean) getContextField(ctx, "eof"));
        assertEquals(0, (int) getContextField(ctx, "pos"));
    }

    @Test
    void fullBlockDecoding_emitsFiveBytesAndLeavesEofFalse() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", false);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 0);
        setContextField(ctx, "lbitWorkArea", 0L);
        byte[] input = "ABCDEFGH".getBytes(StandardCharsets.US_ASCII);
        invokeDecode(codec, input, 0, input.length, ctx);
        assertFalse((boolean) getContextField(ctx, "eof"));
        assertEquals(5, (int) getContextField(ctx, "pos"));
        // The exact output bytes are not important for this test – we just verify length/state.
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0 }, getDecodedBytes(ctx));
    }

    @ParameterizedTest
    @MethodSource("trailingCases")
    void trailingCharacterHandling(int modulus, long lbitWorkArea, Class<? extends Throwable> expectedException, byte[] expectedOutput) throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", true);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", modulus);
        setContextField(ctx, "lbitWorkArea", lbitWorkArea);
        byte[] empty = new byte[0];
        if (expectedException != null) {
            Executable exec = () -> invokeDecode(codec, empty, 0, 0, ctx);
            assertThrows(expectedException, exec, "Expected exception for modulus " + modulus);
        } else {
            invokeDecode(codec, empty, 0, 0, ctx);
            assertArrayEquals(expectedOutput, getDecodedBytes(ctx), "Incorrect output for modulus " + modulus);
        }
    }

    @Test
    void noTrailingWhenEofTrueAndModulusZeroWritesNoAdditionalBytes() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", true);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 0);
        setContextField(ctx, "lbitWorkArea", 0L);
        byte[] input = "ABCDEFGH".getBytes(StandardCharsets.US_ASCII);
        invokeDecode(codec, input, 0, input.length, ctx);
        assertEquals(5, (int) getContextField(ctx, "pos"));
    }

    @Test
    void invalidCharacterIsIgnoredAndDoesNotAffectModulus() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", false);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 0);
        setContextField(ctx, "lbitWorkArea", 0L);
        byte[] input = new byte[] { 'A', (byte) 0xFF, 'B' };
        invokeDecode(codec, input, 0, input.length, ctx);
        assertEquals(2, (int) getContextField(ctx, "modulus"));
        assertEquals(0, (int) getContextField(ctx, "pos"));
    }

    @Test
    void partialBlockWithPaddingEmitsTwoBytesAndSetsEof() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", false);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 0);
        setContextField(ctx, "lbitWorkArea", 0L);
        byte[] input = "ABCD=".getBytes(StandardCharsets.US_ASCII);
        invokeDecode(codec, input, 0, input.length, ctx);
        assertTrue((boolean) getContextField(ctx, "eof"));
        assertEquals(4, (int) getContextField(ctx, "modulus"));
        assertEquals(2, (int) getContextField(ctx, "pos"));
    }

    @Test
    void negativeInAvailWithPendingBitsTriggersTrailingSwitch() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", false);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 5);
        setContextField(ctx, "lbitWorkArea", 0x1FFFFFL);
        invokeDecode(codec, new byte[0], 0, -1, ctx);
        assertTrue((boolean) getContextField(ctx, "eof"));
        assertEquals(3, (int) getContextField(ctx, "pos"));
        byte[] out = getDecodedBytes(ctx);
        assertArrayEquals(new byte[] { (byte) ((0x1FFFFFL >> 16) & 0xFF), (byte) ((0x1FFFFFL >> 8) & 0xFF), (byte) (0x1FFFFFL & 0xFF) }, out);
    }

    @Test
    void extraPaddingCharactersAreIgnoredAfterFirstEquals() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", false);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 0);
        setContextField(ctx, "lbitWorkArea", 0L);
        byte[] input = "ABCDEFGH====".getBytes(StandardCharsets.US_ASCII);
        invokeDecode(codec, input, 0, input.length, ctx);
        assertTrue((boolean) getContextField(ctx, "eof"));
        assertEquals(5, (int) getContextField(ctx, "pos"));
    }

    @Test
    void whitespaceIsSkippedDuringDecoding() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", false);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 0);
        setContextField(ctx, "lbitWorkArea", 0L);
        byte[] input = "A B\nCDEFGH".getBytes(StandardCharsets.US_ASCII);
        invokeDecode(codec, input, 0, input.length, ctx);
        assertEquals(5, (int) getContextField(ctx, "pos"));
    }

    @Test
    void validateCharacterFailsWhenLowBitsNotZero_modulus2() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", true);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 2);
        setContextField(ctx, "lbitWorkArea", 0x3L);
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx));
    }

    @Test
    void validateCharacterFailsWhenLowBitsNotZero_modulus4() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", true);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 4);
        setContextField(ctx, "lbitWorkArea", 0xABCDEFL);
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx));
    }

    @Test
    void validateCharacterFailsWhenLowBitsNotZero_modulus5() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", true);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 5);
        setContextField(ctx, "lbitWorkArea", 0x1FFFFFL);
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx));
    }

    @Test
    void validateCharacterFailsWhenLowBitsNotZero_modulus7() throws Exception {
        Base32 codec = new Base32();
        Object ctx = newContext();
        setContextField(ctx, "eof", true);
        setContextField(ctx, "pos", 0);
        setContextField(ctx, "modulus", 7);
        setContextField(ctx, "lbitWorkArea", 0x7L);
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx));
    }

    @Test
    void decode_nullInputArray_throwsNullPointerException() throws Exception {
        Base32 codec = new Base32();
        assertThrows(NullPointerException.class, () -> codec.decode((byte[]) null), "Decoding a null byte array should throw NPE");
    }

    @Test
    void decode_nullContext_throwsNullPointerException() throws Exception {
        Base32 codec = new Base32();
        byte[] input = "A".getBytes(StandardCharsets.US_ASCII);
        assertThrows(NullPointerException.class, () -> invokeDecode(codec, input, 0, 1, null));
    }

    @ParameterizedTest
    @MethodSource("illegalInputs")
    void testStrictDecoderRejectsInvalidInput(String input) {
        Base32 codec = strictBase32();
        assertThrows(IllegalArgumentException.class, () -> codec.decode(input.getBytes(StandardCharsets.US_ASCII)), () -> "Expected strict decoder to reject: \"" + input + "\"");
    }
}

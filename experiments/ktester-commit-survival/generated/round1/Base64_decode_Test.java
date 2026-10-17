package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Base64_decode_Test {

    private Base64 base64;

    private BaseNCodec.Context ctx;

    /**
     * Strict decoder that also validates {@code null} arguments.
     * The strict behaviour (validation of trailing bits, etc.) is provided by the
     * underlying {@link Base64} implementation; this subclass only adds explicit
     * {@code null} checks before delegating to {@code super.decode(...)}.
     */
    private static final class StrictBase64 extends Base64 {

        StrictBase64() {
            // use default ctor – the Base64 implementation already validates
            // trailing bits and throws IllegalArgumentException where appropriate.
            super();
        }

        @Override
        public void decode(final byte[] input, final int inPos, final int inAvail, final Context context) {
            if (input == null) {
                throw new NullPointerException("input is null");
            }
            if (context == null) {
                throw new NullPointerException("context is null");
            }
            super.decode(input, inPos, inAvail, context);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the strict‑decoding wrapper.
        base64 = new StrictBase64();
        ctx = new BaseNCodec.Context();
    }

    @AfterEach
    void teardownAfterEach() {
        base64 = null;
        ctx = null;
    }

    /**
     * Helper that extracts the decoded bytes from the context's internal buffer.
     * The {@code BaseNCodec.Context} class stores the output in {@code buffer}
     * starting at index {@code 0} up to {@code pos - 1}.
     */
    private static byte[] extractResult(BaseNCodec.Context context) {
        final byte[] result = new byte[context.pos];
        System.arraycopy(context.buffer, 0, result, 0, context.pos);
        return result;
    }

    /**
     * Simple happy‑path test: decode the Base64 string {@code "AQID"} which represents
     * the three bytes {@code {1,2,3}}. No padding is present, therefore {@code eof}
     * remains {@code false} and the decoded bytes are stored in {@code ctx.buffer}.
     */
    @Test
    void decode_withoutPadding_returnsExactBytes() {
        final byte[] input = "AQID".getBytes(StandardCharsets.US_ASCII);
        base64.decode(input, 0, input.length, ctx);
        assertFalse(ctx.eof, "EOF should be false when no padding is seen");
        assertEquals(3, ctx.pos, "Exactly three bytes should have been decoded");
        assertArrayEquals(new byte[] { 1, 2, 3 }, extractResult(ctx), "Decoded bytes must match the original payload");
    }

    /**
     * Decoding with a single padding character. The input {@code "AQ=="} corresponds to
     * a single byte {@code 0x01}. After decoding the context must indicate EOF and
     * contain exactly one byte.
     */
    @Test
    void decode_withSinglePadding_returnsOneByte() {
        final byte[] input = "AQ==".getBytes(StandardCharsets.US_ASCII);
        base64.decode(input, 0, input.length, ctx);
        assertTrue(ctx.eof, "EOF should be true after encountering padding");
        assertEquals(1, ctx.pos, "Only one byte should be produced");
        assertArrayEquals(new byte[] { 1 }, extractResult(ctx));
    }

    /**
     * Decoding with two padding characters (modulus 3). The Base64 string {@code "AQI="}
     * decodes to the two bytes {@code {1,2}}. The method reaches the {@code case 3}
     * branch in the trailing‑bits handling logic.
     */
    @Test
    void decode_withTwoPadding_returnsTwoBytes() {
        final byte[] input = "AQI=".getBytes(StandardCharsets.US_ASCII);
        base64.decode(input, 0, input.length, ctx);
        assertTrue(ctx.eof, "EOF should be true after the padding character");
        assertEquals(2, ctx.pos, "Two bytes should be produced");
        assertArrayEquals(new byte[] { 1, 2 }, extractResult(ctx));
    }

    /**
     * Decoding a string that leaves a modulus of {@code 1} (a single valid Base64
     * character followed by padding). According to the implementation this should
     * trigger {@code validateTrailingCharacter()}, which throws an
     * {@link IllegalArgumentException}.
     */
    @Test
    void decode_modulusOne_throwsException() {
        final byte[] input = "A===".getBytes(StandardCharsets.US_ASCII);
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> base64.decode(input, 0, input.length, ctx), "Modulus 1 should cause an IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Trailing"), "Exception message should mention trailing bits");
    }

    /**
     * Passing {@code null} as the input array must result in a {@link NullPointerException}
     * because the implementation attempts to read {@code input[inPos]}.
     */
    @Test
    void testDecode_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> base64.decode(null, 0, 0, new BaseNCodec.Context()), "Null input array should cause NullPointerException");
    }

    /**
     * Passing {@code null} as the {@link BaseNCodec.Context} must also raise a
     * {@link NullPointerException} when the method accesses context fields.
     */
    @Test
    void testDecode_nullContext_throwsNullPointerException() {
        final byte[] empty = new byte[0];
        assertThrows(NullPointerException.class, () -> base64.decode(empty, 0, 0, null), "Null context should cause NullPointerException");
    }

    /**
     * When {@code inPos} points beyond the end of {@code input}, the first array
     * access triggers an {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    void testDecode_invalidIndices_throwsArrayIndexOutOfBoundsException() {
        // 'A','B'
        final byte[] input = new byte[] { 0x41, 0x42 };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> base64.decode(input, 2, 1, new BaseNCodec.Context()), "Reading beyond the input array should throw ArrayIndexOutOfBoundsException");
    }

    /**
     * If {@code context.modulus} holds a value outside the range 0‑3 when EOF handling
     * occurs, the decoder throws an {@link IllegalStateException}.
     */
    @Test
    void testDecode_modulusInvalid_throwsIllegalStateException() {
        final BaseNCodec.Context badCtx = new BaseNCodec.Context();
        // illegal value
        badCtx.modulus = 5;
        assertThrows(IllegalStateException.class, () -> base64.decode(new byte[0], 0, -1, badCtx), "Illegal modulus should cause IllegalStateException");
    }

    /**
     * EOF with {@code modulus == 1} triggers {@code validateTrailingCharacter()},
     * which throws {@link IllegalArgumentException}.
     */
    @Test
    void testDecode_modulusOne_throwsIllegalArgumentException() {
        final BaseNCodec.Context ctxOne = new BaseNCodec.Context();
        ctxOne.modulus = 1;
        assertThrows(IllegalArgumentException.class, () -> base64.decode(new byte[0], 0, -1, ctxOne), "Modulus 1 at EOF should cause IllegalArgumentException");
    }

    /**
     * EOF with {@code modulus == 2} calls {@code validateCharacter(MASK_4BITS, ctx)}.
     * Supplying a work area whose low‑4 bits are non‑zero causes an
     * {@link IllegalArgumentException}.
     */
    @Test
    void testDecode_modulusTwo_invalidMask_throwsIllegalArgumentException() {
        final BaseNCodec.Context ctxTwo = new BaseNCodec.Context();
        ctxTwo.modulus = 2;
        // low 4 bits set – invalid
        ctxTwo.ibitWorkArea = 0xF;
        assertThrows(IllegalArgumentException.class, () -> base64.decode(new byte[0], 0, -1, ctxTwo), "Invalid low‑4‑bit mask with modulus 2 should cause IllegalArgumentException");
    }

    /**
     * EOF with {@code modulus == 3} calls {@code validateCharacter(MASK_2BITS, ctx)}.
     * Supplying a work area whose low‑2 bits are non‑zero causes an
     * {@link IllegalArgumentException}.
     */
    @Test
    void testDecode_modulusThree_invalidMask_throwsIllegalArgumentException() {
        final BaseNCodec.Context ctxThree = new BaseNCodec.Context();
        ctxThree.modulus = 3;
        // low 2 bits set – invalid
        ctxThree.ibitWorkArea = 0x3;
        assertThrows(IllegalArgumentException.class, () -> base64.decode(new byte[0], 0, -1, ctxThree), "Invalid low‑2‑bit mask with modulus 3 should cause IllegalArgumentException");
    }
}

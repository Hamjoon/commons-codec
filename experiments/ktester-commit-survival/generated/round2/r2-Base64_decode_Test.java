package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.commons.codec.CodecPolicy;

@ExtendWith(MockitoExtension.class)
class Base64_decode_Test {

    /**
     * Codec with default (lenient) policy.
     */
    private Base64 lenientCodec;

    /**
     * Codec with strict policy – used to verify validation of illegal trailing bits.
     */
    private Base64 strictCodec;

    /**
     * Re‑used context for each test.
     */
    private BaseNCodec.Context context;

    @BeforeEach
    void setupBeforeEach() {
        // default (lenient) codec
        lenientCodec = new Base64();
        // strict codec – required for the “impossible trailing bits” tests
        strictCodec = new Base64(0, null, false, CodecPolicy.STRICT);
        // fresh context for every test case
        context = new BaseNCodec.Context();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        lenientCodec = null;
        strictCodec = null;
        context = null;
    }

    /**
     * Simple happy‑path test: decode a Base64 string with padding.
     * Expected result is the ASCII bytes for “Hello”.
     */
    @Test
    void decode_WithPadding_ReturnsExpectedBytes() {
        final byte[] input = "SGVsbG8=".getBytes(StandardCharsets.US_ASCII);
        lenientCodec.decode(input, 0, input.length, context);
        // Signal EOF so the decoder can flush any remaining bits.
        lenientCodec.decode(input, 0, -1, context);
        final byte[] actual = Arrays.copyOf(context.buffer, context.pos);
        final byte[] expected = "Hello".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, actual);
    }

    /**
     * Verify that the decoder works when the padding character is omitted.
     * RFC 2045 permits the final “=” to be absent.
     */
    @Test
    void decode_WithoutPadding_ReturnsExpectedBytes() {
        final byte[] input = "SGVsbG8".getBytes(StandardCharsets.US_ASCII);
        lenientCodec.decode(input, 0, input.length, context);
        // Must call with inAvail = -1 to flush the final, incomplete block.
        lenientCodec.decode(input, 0, -1, context);
        final byte[] actual = Arrays.copyOf(context.buffer, context.pos);
        final byte[] expected = "Hello".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, actual);
    }

    /**
     * In strict decoding mode, illegal trailing bits must cause a validation
     * exception. The string “AB” is one of the documented impossible cases.
     */
    @Test
    void decode_StrictMode_InvalidTrailingBits_ThrowsException() {
        final byte[] input = "AB".getBytes(StandardCharsets.US_ASCII);
        // First decode the data …
        strictCodec.decode(input, 0, input.length, context);
        // … then signal EOF so the strict validator runs.
        assertThrows(IllegalArgumentException.class, () -> strictCodec.decode(input, 0, -1, context));
    }

    /**
     * When the caller signals end‑of‑stream with a negative {@code inAvail},
     * the decoder must set {@code context.eof} and stop further processing.
     */
    @Test
    void decode_NegativeInAvail_SetsEofFlag() {
        final byte[] input = "SGVsbG8=".getBytes(StandardCharsets.US_ASCII);
        lenientCodec.decode(input, 0, input.length, context);
        // Signal EOF
        lenientCodec.decode(input, 0, -1, context);
        assertTrue(context.eof, "Context should be marked as EOF after negative inAvail");
    }

    /**
     * Verify that the decoder correctly processes a multi‑block input and
     * respects the internal buffer growth logic.
     */
    @Test
    void decode_MultiBlockInput_BufferGrowsAsNeeded() {
        final String original = "The quick brown fox jumps over the lazy dog. " + "Pack my box with five dozen liquor jugs.";
        final byte[] encoded = Base64.encodeBase64(original.getBytes(StandardCharsets.UTF_8));
        lenientCodec.decode(encoded, 0, encoded.length, context);
        lenientCodec.decode(encoded, 0, -1, context);
        final byte[] decoded = Arrays.copyOf(context.buffer, context.pos);
        assertArrayEquals(original.getBytes(StandardCharsets.UTF_8), decoded);
    }

    /**
     * Early EOF – the method should return immediately when {@code context.eof} is already true.
     */
    @Test
    void decode_EarlyEof_ReturnsImmediately() {
        context.eof = true;
        context.pos = 0;
        final byte[] input = "AB".getBytes(StandardCharsets.US_ASCII);
        lenientCodec.decode(input, 0, input.length, context);
        assertTrue(context.eof, "EOF flag must remain true");
        assertEquals(0, context.pos, "No bytes should have been written");
        assertNull(context.buffer, "Buffer should stay null because nothing was allocated");
    }

    /**
     * Full block decoding – four valid Base64 characters produce three output bytes.
     */
    @Test
    void decode_FullBlock_ProducesThreeBytes() {
        final byte[] input = Base64.encodeBase64(new byte[] { 0x01, 0x02, 0x03 });
        lenientCodec.decode(input, 0, input.length, context);
        lenientCodec.decode(input, 0, -1, context);
        final byte[] actual = Arrays.copyOf(context.buffer, context.pos);
        final byte[] expected = new byte[] { 0x01, 0x02, 0x03 };
        assertArrayEquals(expected, actual);
    }

    /**
     * Trailing bits – modulus == 1 (invalid). The decoder must invoke {@code validateTrailingCharacter()}
     * which throws {@link IllegalArgumentException}.
     */
    @Test
    void decode_TrailingBits_Modulus1_ThrowsException() {
        // Process a single Base64 character ('A' → index 0) which leaves modulus == 1.
        final byte[] input = "A".getBytes(StandardCharsets.US_ASCII);
        strictCodec.decode(input, 0, input.length, context);
        // Signal EOF – this should trigger validation and throw.
        assertThrows(IllegalArgumentException.class, () -> strictCodec.decode(new byte[0], 0, -1, context));
    }

    /**
     * Trailing bits – modulus == 2 (produces one byte). Choose characters whose low‑4‑bits are zero
     * (e.g., "AQ" → indices 0 and 16). After signalling EOF the decoder should output a single byte {@code 0x01}.
     */
    @Test
    void decode_TrailingBits_Modulus2_ProducesOneByte() {
        final byte[] input = "AQ".getBytes(StandardCharsets.US_ASCII);
        lenientCodec.decode(input, 0, input.length, context);
        lenientCodec.decode(new byte[0], 0, -1, context);
        final byte[] actual = Arrays.copyOf(context.buffer, context.pos);
        assertEquals(1, actual.length, "Exactly one byte should be produced");
        assertEquals((byte) 0x01, actual[0], "Decoded byte should be 0x01");
    }

    /**
     * Trailing bits – modulus == 3 (produces two bytes). Choose characters whose low‑2‑bits are zero
     * (e.g., "AQg" → indices 0, 16, 32). Expected output bytes are {@code 0x01, 0x08}.
     */
    @Test
    void decode_TrailingBits_Modulus3_ProducesTwoBytes() {
        final byte[] input = "AQg".getBytes(StandardCharsets.US_ASCII);
        lenientCodec.decode(input, 0, input.length, context);
        lenientCodec.decode(new byte[0], 0, -1, context);
        final byte[] actual = Arrays.copyOf(context.buffer, context.pos);
        assertArrayEquals(new byte[] { 0x01, 0x08 }, actual);
    }

    /**
     * Invalid modulus – forces the default branch of the switch and should throw {@link IllegalStateException}.
     */
    @Test
    void decode_InvalidModulus_ThrowsIllegalStateException() {
        // Manually craft an impossible state (modulus = 5) without pre‑setting eof.
        context.modulus = 5;
        context.ibitWorkArea = 0;
        // The decoder should notice the illegal state when EOF is signalled.
        assertThrows(IllegalStateException.class, () -> lenientCodec.decode(new byte[0], 0, -1, context));
    }

    /**
     * Non‑alphabet characters – they must be ignored. The 0xFF byte is not part of the Base64 alphabet.
     */
    @Test
    void decode_NonAlphabetCharacters_Ignored() {
        final byte[] input = new byte[] { 'A', 'B', (byte) 0xFF, 'C', 'D' };
        lenientCodec.decode(input, 0, input.length, context);
        lenientCodec.decode(input, 0, -1, context);
        final byte[] actual = Arrays.copyOf(context.buffer, context.pos);
        final byte[] expected = Base64.decodeBase64(new byte[] { 'A', 'B', 'C', 'D' });
        assertArrayEquals(expected, actual);
    }

    /**
     * Null input array – should throw {@link NullPointerException}.
     */
    @Test
    void decode_NullInput_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> lenientCodec.decode(null, 0, 1, context));
    }

    /**
     * Null context – should throw {@link NullPointerException}.
     */
    @Test
    void decode_NullContext_ThrowsNullPointerException() {
        final byte[] input = "A".getBytes(StandardCharsets.US_ASCII);
        assertThrows(NullPointerException.class, () -> lenientCodec.decode(input, 0, 1, null));
    }

    /**
     * {@code inPos} out of bounds – should throw {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    void decode_InPosOutOfBounds_ThrowsArrayIndexOutOfBoundsException() {
        final byte[] input = new byte[] { 'A', 'B', 'C', 'D' };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> lenientCodec.decode(input, 5, 1, context));
    }

    /**
     * Strict mode – {@code validateCharacter} throws for non‑zero low 4 bits (modulus == 2).
     */
    @Test
    void decode_Strict_Modulus2_NonZeroLowBits_ThrowsException() {
        // Simulate having processed two characters whose combined low‑4‑bits are non‑zero.
        // We do NOT set eof manually – the decoder will set it after the -1 call.
        context.modulus = 2;
        // low 4 bits != 0
        context.ibitWorkArea = 0b0000_1111;
        assertThrows(IllegalArgumentException.class, () -> strictCodec.decode(new byte[0], 0, -1, context));
    }

    /**
     * Strict mode – {@code validateCharacter} throws for non‑zero low 2 bits (modulus == 3).
     */
    @Test
    void decode_Strict_Modulus3_NonZeroLowBits_ThrowsException() {
        context.modulus = 3;
        // low 2 bits != 0
        context.ibitWorkArea = 0b0000_0101;
        assertThrows(IllegalArgumentException.class, () -> strictCodec.decode(new byte[0], 0, -1, context));
    }
}
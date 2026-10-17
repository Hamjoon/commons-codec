package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;

// kept to satisfy the provided template
@ExtendWith(MockitoExtension.class)
class Base32_decode_Test {

    /**
     * Charset used for converting between {@code String} and {@code byte[]}.
     */
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * Test vectors taken from RFC‑4648 (plain Base32).
     */
    private static final String[][] BASE32_TEST_CASES = { { "", "" }, { "f", "MY======" }, { "fo", "MZXQ====" }, { "foo", "MZXW6===" }, { "foob", "MZXW6YQ=" }, { "fooba", "MZXW6YTB" }, { "foobar", "MZXW6YTBOI======" } };

    /**
     * Line separator used for chunked encoding tests.
     */
    private static final byte[] CRLF = "\r\n".getBytes(UTF8);

    /**
     * Helper that reflects the package‑protected {@code decode} method.
     */
    private static void invokeDecode(Base32 codec, byte[] input, int inPos, int inAvail, BaseNCodec.Context context) throws Exception {
        Method m = Base32.class.getDeclaredMethod("decode", byte[].class, int.class, int.class, BaseNCodec.Context.class);
        m.setAccessible(true);
        m.invoke(codec, input, inPos, inAvail, context);
    }

    /**
     * Verify that decoding the RFC‑4648 examples yields the original byte arrays.
     */
    @Test
    void testDecodeStandardVectors() {
        for (String[] pair : BASE32_TEST_CASES) {
            String original = pair[0];
            String encoded = pair[1];
            // default (no line breaks, no hex)
            Base32 codec = new Base32();
            byte[] decoded = codec.decode(encoded.getBytes(UTF8));
            assertArrayEquals(original.getBytes(UTF8), decoded, () -> "Failed decoding \"" + encoded + "\"");
        }
    }

    /**
     * Ensure that the decoder correctly handles line‑separator characters when a
     * line length is configured. The encoded data contains CRLF after every
     * 10 characters (the same line length used in the original test suite).
     */
    @Test
    void testDecodeWithChunkedInput() {
        // Encode a longer string with line length = 10 so that CRLFs are inserted.
        String longPlain = "The quick brown fox jumps over the lazy dog.";
        // line length = 10, default CRLF separator
        Base32 encoder = new Base32(10);
        byte[] encoded = encoder.encode(longPlain.getBytes(UTF8));
        // Decoder must be able to handle the same chunked representation.
        Base32 decoder = new Base32(10);
        byte[] decoded = decoder.decode(encoded);
        assertArrayEquals(longPlain.getBytes(UTF8), decoded, "Chunked Base32 decoding did not produce the original bytes");
    }

    /**
     * Verify that the hex variant of Base32 can decode data that was encoded with
     * the same variant. The test uses a small payload to keep the expected value
     * readable.
     */
    @Test
    void testDecodeHexVariant() {
        String plain = "Base32Hex";
        // useHex = true
        Base32 hexEncoder = new Base32(true);
        byte[] encoded = hexEncoder.encode(plain.getBytes(UTF8));
        Base32 hexDecoder = new Base32(true);
        byte[] decoded = hexDecoder.decode(encoded);
        assertArrayEquals(plain.getBytes(UTF8), decoded, "Hex variant decoding failed");
    }

    /**
     * Decoding should stop at the first padding character ('='). Anything after
     * the padding must be ignored. This test feeds an encoded string that contains
     * valid data followed by extra characters after the padding.
     */
    @Test
    void testDecodeStopsAtFirstPadding() {
        // "MZXW6===" decodes to "foo". Append garbage after the padding.
        String encodedWithGarbage = "MZXW6===XYZ";
        Base32 codec = new Base32();
        byte[] decoded = codec.decode(encodedWithGarbage.getBytes(UTF8));
        assertArrayEquals("foo".getBytes(UTF8), decoded, "Decoder did not stop at the first padding character");
    }

    /**
     * The decoder should gracefully handle {@code null} input by returning {@code null}.
     */
    @Test
    void testDecodeNullInput() {
        Base32 codec = new Base32();
        assertNull(codec.decode((byte[]) null), "Decoding null should return null");
    }

    /**
     * Verify that an impossible trailing‑character combination (e.g. a single
     * non‑padding character at the end) does not corrupt previously decoded data.
     * According to the implementation such cases are ignored after validation.
     */
    @Test
    void testDecodeImpossibleTrailingCharactersAreIgnored() {
        // "MC======" is an impossible combination (only 2 bits of data).
        String impossible = "MC======";
        Base32 codec = new Base32();
        // The method should not throw; it should simply return an empty array.
        byte[] result = codec.decode(impossible.getBytes(UTF8));
        assertEquals(0, result.length, "Impossible trailing characters should result in an empty output");
    }

    /**
     * When the internal {@code modulus} field contains an illegal value (e.g. 9)
     * the decoder reaches the {@code default} branch of the switch and throws
     * {@link IllegalStateException}.
     */
    @Test
    void testDecodeInvalidModulus() throws Exception {
        Base32 codec = new Base32();
        byte[] empty = new byte[0];
        BaseNCodec.Context ctx = new BaseNCodec.Context();
        // forces the EOF‑handling block
        ctx.eof = true;
        // illegal value
        ctx.modulus = 9;
        ctx.lbitWorkArea = 0L;
        ctx.pos = 0;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> invokeDecode(codec, empty, 0, 0, ctx), "Expected IllegalStateException for impossible modulus");
        assertTrue(ex.getMessage().contains("Impossible modulus 9"), "Exception message should mention the illegal modulus");
    }

    /**
     * Modulus 1 triggers {@code validateTrailingCharacters()}, which must reject
     * the leftover 5 bits and throw an {@link IllegalArgumentException}.
     */
    @Test
    void testDecodeValidateTrailingCharacters_mod1() throws Exception {
        Base32 codec = new Base32();
        BaseNCodec.Context ctx = new BaseNCodec.Context();
        ctx.eof = true;
        ctx.modulus = 1;
        ctx.lbitWorkArea = 0L;
        ctx.pos = 0;
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx), "Modulus 1 should cause validateTrailingCharacters() to throw");
    }

    /**
     * Modulus 3 also calls {@code validateTrailingCharacters()} and must raise
     * an {@link IllegalArgumentException}.
     */
    @Test
    void testDecodeValidateTrailingCharacters_mod3() throws Exception {
        Base32 codec = new Base32();
        BaseNCodec.Context ctx = new BaseNCodec.Context();
        ctx.eof = true;
        ctx.modulus = 3;
        ctx.lbitWorkArea = 0L;
        ctx.pos = 0;
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx), "Modulus 3 should cause validateTrailingCharacters() to throw");
    }

    /**
     * Modulus 6 calls {@code validateTrailingCharacters()} as well.
     */
    @Test
    void testDecodeValidateTrailingCharacters_mod6() throws Exception {
        Base32 codec = new Base32();
        BaseNCodec.Context ctx = new BaseNCodec.Context();
        ctx.eof = true;
        ctx.modulus = 6;
        ctx.lbitWorkArea = 0L;
        ctx.pos = 0;
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx), "Modulus 6 should cause validateTrailingCharacters() to throw");
    }

    /**
     * Modulus 2 expects the two least‑significant bits to be zero. Providing
     * {@code 0b11} violates this contract and forces {@code validateCharacter}
     * to throw.
     */
    @Test
    void testDecodeValidateCharacter_mod2_invalidBits() throws Exception {
        Base32 codec = new Base32();
        BaseNCodec.Context ctx = new BaseNCodec.Context();
        ctx.eof = true;
        ctx.modulus = 2;
        // non‑zero low 2 bits
        ctx.lbitWorkArea = 0b11L;
        ctx.pos = 0;
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx), "Modulus 2 with non‑zero low bits should cause validateCharacter to throw");
    }

    /**
     * Modulus 4 expects the low 4 bits to be zero. Supplying {@code 0b1111}
     * triggers the validation failure.
     */
    @Test
    void testDecodeValidateCharacter_mod4_invalidBits() throws Exception {
        Base32 codec = new Base32();
        BaseNCodec.Context ctx = new BaseNCodec.Context();
        ctx.eof = true;
        ctx.modulus = 4;
        // non‑zero low 4 bits
        ctx.lbitWorkArea = 0b1111L;
        ctx.pos = 0;
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx), "Modulus 4 with non‑zero low bits should cause validateCharacter to throw");
    }

    /**
     * Modulus 5 expects the lowest bit to be zero. Providing {@code 0b1}
     * violates the rule.
     */
    @Test
    void testDecodeValidateCharacter_mod5_invalidBits() throws Exception {
        Base32 codec = new Base32();
        BaseNCodec.Context ctx = new BaseNCodec.Context();
        ctx.eof = true;
        ctx.modulus = 5;
        // non‑zero low bit
        ctx.lbitWorkArea = 0b1L;
        ctx.pos = 0;
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx), "Modulus 5 with non‑zero low bit should cause validateCharacter to throw");
    }

    /**
     * Modulus 7 expects the low 3 bits to be zero. Supplying {@code 0b111}
     * triggers the validation error.
     */
    @Test
    void testDecodeValidateCharacter_mod7_invalidBits() throws Exception {
        Base32 codec = new Base32();
        BaseNCodec.Context ctx = new BaseNCodec.Context();
        ctx.eof = true;
        ctx.modulus = 7;
        // non‑zero low 3 bits
        ctx.lbitWorkArea = 0b111L;
        ctx.pos = 0;
        assertThrows(IllegalArgumentException.class, () -> invokeDecode(codec, new byte[0], 0, 0, ctx), "Modulus 7 with non‑zero low bits should cause validateCharacter to throw");
    }
}

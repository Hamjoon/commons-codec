package org.apache.commons.codec.binary;

import org.apache.commons.codec.CodecPolicy;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class Base64_builder_Test {

    /**
     * Finds a method with the given name and parameter types on the builder's
     * concrete class. If the method does not exist, an {@link AssertionError}
     * is thrown to fail the test clearly.
     */
    private static Method findMethod(Object target, String name, Class<?>... paramTypes) {
        try {
            Method m = target.getClass().getMethod(name, paramTypes);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Expected method '" + name + "' not found on Builder class", e);
        }
    }

    /**
     * Invokes a method via reflection, re‑throwing any checked exception as a
     * runtime exception to keep test code concise.
     */
    private static Object invoke(Object target, Method method, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ite) {
            // Unwrap the underlying exception – the test framework expects the
            // original exception type (e.g., NullPointerException).
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The {@code builder()} method must return a non‑null {@link Base64.Builder}
     * instance.
     */
    @Test
    void testBuilderIsNotNull() {
        Base64.Builder builder = Base64.builder();
        assertNotNull(builder, "Base64.builder() should not return null");
        assertTrue(builder instanceof Base64.Builder, "Base64.builder() should return an instance of Base64.Builder");
    }

    /**
     * A builder without any customisation must produce a {@link Base64}
     * instance that behaves like the default constructor (lenient decoding,
     * not URL‑safe, standard padding character '=').
     */
    @Test
    void testBuilderDefaultConfiguration() {
        Base64 codecFromBuilder = Base64.builder().get();
        // default constructor
        Base64 codecFromDefaultCtor = new Base64();
        // Both instances should be lenient and not URL‑safe.
        assertFalse(codecFromBuilder.isUrlSafe(), "Default builder should produce a non‑URL‑safe codec");
        assertFalse(codecFromDefaultCtor.isUrlSafe(), "Default ctor should produce a non‑URL‑safe codec");
        assertEquals(CodecPolicy.LENIENT, codecFromBuilder.getCodecPolicy(), "Default builder should use LENIENT decoding policy");
        assertEquals(CodecPolicy.LENIENT, codecFromDefaultCtor.getCodecPolicy(), "Default ctor should use LENIENT decoding policy");
        // Encode a known payload and compare with the static convenience method.
        byte[] payload = "Apache".getBytes();
        byte[] expected = Base64.encodeBase64(payload);
        byte[] actual = codecFromBuilder.encode(payload);
        assertArrayEquals(expected, actual, "Encoding with builder‑created codec must match static method");
    }

    /**
     * Verify that custom settings supplied via the builder are honoured.
     * The test customises:
     * <ul>
     *   <li>URL‑safe mode</li>
     *   <li>Line length and line separator</li>
     *   <li>Padding character</li>
     *   <li>Decoding policy (STRICT)</li>
     * </ul>
     */
    @Test
    void testBuilderCustomConfiguration() {
        // Build a Base64 instance with a non‑standard configuration.
        Base64 codec = Base64.builder().setUrlSafe(// URL‑safe mode
        true).setLineLength(// short line length to force breaks
        4).setLineSeparator('\r', // CRLF separator
        '\n').setPadding(// custom padding character
        '#').setDecodingPolicy(// strict decoding
        CodecPolicy.STRICT).get();
        // Verify the configuration via the public API.
        assertTrue(codec.isUrlSafe(), "Builder should have enabled URL‑safe mode");
        assertEquals(CodecPolicy.STRICT, codec.getCodecPolicy(), "Builder should have set strict decoding policy");
        // -----------------------------------------------------------------
        // 1. Padding character – encode a single byte which requires padding.
        //    Standard Base64 for "a" is "YQ==". With padding '#', we expect "YQ##".
        // -----------------------------------------------------------------
        byte[] singleByte = "a".getBytes();
        byte[] encoded = codec.encode(singleByte);
        String encodedStr = new String(encoded);
        assertEquals("YQ##", encodedStr, "Custom padding character should be used");
        // -----------------------------------------------------------------
        // 2. Line length & separator – encode data that exceeds 4 characters.
        //    Use "foobar" (6 bytes) which yields 8 Base64 chars: "Zm9vYmFy".
        //    With lineLength=4 and CRLF separator we expect:
        //        "Zm9v\r\nYmFy"
        // -----------------------------------------------------------------
        byte[] data = "foobar".getBytes();
        byte[] encodedWithBreaks = codec.encode(data);
        String expectedWithBreaks = "Zm9v\r\nYmFy";
        assertEquals(expectedWithBreaks, new String(encodedWithBreaks), "Line length and separator should be applied");
        // -----------------------------------------------------------------
        // 3. URL‑safe mode – the encoded output must use '-' and '_' instead of
        //    '+' and '/'.
        //    Encode a byte array that contains '+' and '/' in its standard
        //    representation. The bytes {0xfb, 0xef} encode to "+-7/" in standard
        //    Base64; in URL‑safe mode they become "-_7_".
        // -----------------------------------------------------------------
        byte[] urlSensitive = new byte[] { (byte) 0xfb, (byte) 0xef };
        byte[] urlEncoded = codec.encode(urlSensitive);
        String urlEncodedStr = new String(urlEncoded);
        assertFalse(urlEncodedStr.contains("+") || urlEncodedStr.contains("/"), "URL‑safe mode must not contain '+' or '/' characters");
    }

    /**
     * Each call to {@code Base64.builder()} must return a **new** Builder
     * instance (different object reference).
     */
    @Test
    void testBuilderReturnsDistinctInstances() {
        Base64.Builder first = Base64.builder();
        Base64.Builder second = Base64.builder();
        assertNotSame(first, second, "Successive calls should produce distinct Builder objects");
    }

    /**
     * The default configuration produced by the builder must match the
     * expectations of the API: non‑URL‑safe and **no** custom line separator.
     */
    @Test
    void testBuilderDefaultConfigurationDetails() {
        Base64 codec = Base64.builder().get();
        // URL‑safe flag must be false.
        assertFalse(codec.isUrlSafe(), "Default builder should create a non‑URL‑safe codec");
        // The line separator should be {@code null} because the default
        // constructor uses the library's internal default (CRLF) which is not
        // exposed via {@code getLineSeparator()}.
        assertNull(codec.getLineSeparator(), "Default builder should not expose a custom line separator (null expected)");
    }

    /**
     * Builder's {@code setUrlSafe(true)} (or equivalent) must cause the
     * resulting {@link Base64} instance to report {@code true} from
     * {@link Base64#isUrlSafe()}.
     */
    @Test
    void testBuilderUrlSafeOption() {
        Base64 codec = Base64.builder().setUrlSafe(true).get();
        assertTrue(codec.isUrlSafe(), "Builder with URL‑safe flag should produce a codec where isUrlSafe() returns true");
    }

    /**
     * Supplying a custom line separator via the builder must be reflected by
     * {@link Base64#getLineSeparator()}.
     *
     * <p>
     * The builder API uses {@code setLineSeparator(char, char)}; we also
     * verify the underlying byte array.
     * </p>
     */
    @Test
    void testBuilderCustomLineSeparator() {
        // Use CRLF as an explicit custom separator.
        Base64 codec = Base64.builder().setLineSeparator('\r', '\n').get();
        byte[] expected = new byte[] { '\r', '\n' };
        assertArrayEquals(expected, codec.getLineSeparator(), "Custom line separator should be returned unchanged by getLineSeparator()");
    }

    /**
     * {@code withLineSeparator(null)} must throw {@link NullPointerException}.
     */
    @Test
    void testBuilderWithLineSeparatorNullThrowsNPE() throws Exception {
        Base64.Builder builder = Base64.builder();
        Method method = findMethod(builder, "withLineSeparator", byte[].class);
        assertThrows(NullPointerException.class, () -> invoke(builder, method, (Object) null), "Calling withLineSeparator(null) should throw NullPointerException");
    }

    /**
     * {@code withLineSeparator(new byte[]{})} (empty array) must throw
     * {@link IllegalArgumentException}.
     */
    @Test
    void testBuilderWithLineSeparatorEmptyArrayThrowsIAE() throws Exception {
        Base64.Builder builder = Base64.builder();
        Method method = findMethod(builder, "withLineSeparator", byte[].class);
        byte[] empty = new byte[0];
        assertThrows(IllegalArgumentException.class, () -> invoke(builder, method, empty), "Calling withLineSeparator(empty) should throw IllegalArgumentException");
    }

    /**
     * {@code withLineSeparator(new byte[]{'A','B','C'})} – length not a multiple
     * of two – must throw {@link IllegalArgumentException}.
     */
    @Test
    void testBuilderWithLineSeparatorInvalidLengthThrowsIAE() throws Exception {
        Base64.Builder builder = Base64.builder();
        Method method = findMethod(builder, "withLineSeparator", byte[].class);
        byte[] invalid = new byte[] { 'A', 'B', 'C' };
        assertThrows(IllegalArgumentException.class, () -> invoke(builder, method, invalid), "Calling withLineSeparator(non‑multiple‑of‑2) should throw IllegalArgumentException");
    }

    /**
     * After providing an invalid line‑separator length, invoking {@code get()}
     * (or {@code build()}) must result in {@link IllegalStateException}.
     *
     * <p>
     * The builder validates the separator during the build step; we therefore
     * set a malformed separator first and then call {@code get()}.
     * </p>
     */
    @Test
    void testBuilderInvalidSeparatorDuringBuildThrowsISE() throws Exception {
        Base64.Builder builder = Base64.builder();
        Method method = findMethod(builder, "withLineSeparator", byte[].class);
        // 5 bytes – not a multiple of 2, but also not a valid CR/LF pair sequence.
        byte[] malformed = new byte[] { '\r', '\n', '\r', '\n', '\r' };
        invoke(builder, method, malformed);
        assertThrows(IllegalStateException.class, builder::get, "Building after setting an invalid line separator should throw IllegalStateException");
    }
}

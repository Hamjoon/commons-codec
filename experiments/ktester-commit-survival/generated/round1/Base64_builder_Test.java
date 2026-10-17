package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.codec.CodecPolicy;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Base64#builder()} and the {@link Base64.Builder} API.
 *
 * The tests focus on:
 * <ul>
 *   <li>Obtaining a non‑null builder instance.</li>
 *   <li>Verifying the default configuration produced by the builder matches the
 *       behaviour of the no‑arg {@link Base64} constructor.</li>
 *   <li>Checking that builder‑provided configuration methods (URL‑safe flag,
 *       decoding policy, line length, line separator) correctly affect the
 *       resulting {@link Base64} instance.</li>
 * </ul>
 *
 * No private fields or methods are accessed – the tests rely only on the
 * public API of {@link Base64} and {@link Base64.Builder}.
 */
@ExtendWith(MockitoExtension.class)
class Base64_builder_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * The static {@code builder()} method must always return a new, non‑null
     * {@link Base64.Builder} instance.
     */
    @Test
    void testBuilderIsNotNull() {
        Base64.Builder builder = Base64.builder();
        assertNotNull(builder, "Base64.builder() should never return null");
    }

    /**
     * The default builder (no configuration) should produce a {@link Base64}
     * instance that behaves exactly like the no‑arg constructor.
     */
    @Test
    void testBuilderDefaultConfigurationMatchesNoArgConstructor() {
        // Instance created via builder with default settings
        Base64 fromBuilder = Base64.builder().get();

        // Instance created via the public no‑arg constructor
        Base64 fromConstructor = new Base64();

        // Both instances must be URL‑safe false and use the lenient decoding policy.
        assertFalse(fromBuilder.isUrlSafe(), "Default builder should produce a non‑URL‑safe codec");
        assertFalse(fromConstructor.isUrlSafe(), "Default constructor should produce a non‑URL‑safe codec");

        assertFalse(fromBuilder.isStrictDecoding(),
                "Default builder should use lenient decoding (strict = false)");
        assertFalse(fromConstructor.isStrictDecoding(),
                "Default constructor should use lenient decoding (strict = false)");

        // Encode some data with both instances and compare the results.
        byte[] data = "Apache Commons Codec".getBytes(StandardCharsets.UTF_8);
        byte[] encodedFromBuilder = fromBuilder.encode(data);
        byte[] encodedFromConstructor = fromConstructor.encode(data);
        assertArrayEquals(encodedFromConstructor, encodedFromBuilder,
                "Encoding output must be identical for default builder and default constructor");
    }

    /**
     * Verify that the {@code setUrlSafe(true)} flag on the builder results in a
     * {@link Base64} instance whose {@code isUrlSafe()} method returns {@code true}.
     */
    @Test
    void testBuilderUrlSafeFlag() {
        Base64 urlSafeCodec = Base64.builder()
                .setUrlSafe(true)
                .get();

        assertTrue(urlSafeCodec.isUrlSafe(),
                "Builder with setUrlSafe(true) must produce a URL‑safe codec");
    }

    /**
     * Verify that the {@code setDecodingPolicy(CodecPolicy.STRICT)} flag on the
     * builder results in a {@link Base64} instance that performs strict decoding.
     */
    @Test
    void testBuilderDecodingPolicyStrict() {
        Base64 strictCodec = Base64.builder()
                .setDecodingPolicy(CodecPolicy.STRICT)
                .get();

        assertTrue(strictCodec.isStrictDecoding(),
                "Builder with setDecodingPolicy(STRICT) must produce a codec with strict decoding");
        assertEquals(CodecPolicy.STRICT, strictCodec.getCodecPolicy(),
                "CodecPolicy should be STRICT after builder configuration");
    }

    /**
     * Verify that line‑length and line‑separator configuration via the builder
     * matches the behaviour of the corresponding constructor.
     */
    @Test
    void testBuilderLineLengthAndSeparator() {
        // Builder configuration
        Base64 builderConfigured = Base64.builder()
                .setLineLength(4)                     // very small line length to force chunking
                .setLineSeparator('\r', '\n')         // CRLF separator
                .get();

        // Direct constructor configuration (same values)
        Base64 constructorConfigured = new Base64(4, new byte[]{'\r', '\n'});

        // Encode a payload that will be split across several lines.
        byte[] payload = "1234567890".getBytes(StandardCharsets.UTF_8);

        byte[] encodedFromBuilder = builderConfigured.encode(payload);
        byte[] encodedFromConstructor = constructorConfigured.encode(payload);

        assertArrayEquals(encodedFromConstructor, encodedFromBuilder,
                "Builder‑configured line length & separator must match constructor behaviour");
    }
}
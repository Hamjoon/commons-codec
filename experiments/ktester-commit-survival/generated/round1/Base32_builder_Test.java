package org.apache.commons.codec.binary;

import org.apache.commons.codec.CodecPolicy;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base32_builder_Test{

    /** Builder instance created before each test */
    private Base32.Builder builder;

    /** Sample data used for encoding checks */
    private static final byte[] SAMPLE = "foobar".getBytes();

    /** Expected Base32 encoding of {@link #SAMPLE} with the default settings */
    private static final String EXPECTED_DEFAULT = "MZXW6YTBOI======";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a fresh builder for every test case
        builder = Base32.builder();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the builder reference – helps the GC and isolates tests
        builder = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed
    }

    /**
     * Verifies that the default builder creates a {@link Base32} instance that
     * behaves like a freshly‑constructed {@code new Base32()} – i.e. default
     * line length (0), default line separator (CRLF), default padding ('=')
     * and lenient decoding policy.
     */
    @Test
    void defaultBuilderProducesStandardBase32() {
        Base32 base32 = builder.get();               // build the codec
        assertNotNull(base32, "Builder should return a non‑null Base32 instance");

        // Encode the sample data and compare with the RFC‑4648 reference string
        byte[] encoded = base32.encode(SAMPLE);
        assertEquals(EXPECTED_DEFAULT, new String(encoded), "Default builder should produce standard Base32 output");
    }

    /**
     * Verifies that a builder configured with a line length and a line separator
     * produces a chunked (line‑wrapped) output. The test checks that the encoded
     * result contains the expected CRLF separator.
     */
    @Test
    void builderWithChunkingProducesLineSeparatedOutput() {
        // configure line length = 10 (will be rounded to a multiple of 8 internally)
        // and line separator CRLF as shown in the API documentation
        Base32 base32 = builder
                .setLineLength(10)
                .setLineSeparator('\r', '\n')
                .get();

        assertNotNull(base32, "Builder should return a non‑null Base32 instance with chunking");

        byte[] encoded = base32.encode(SAMPLE);
        String encodedStr = new String(encoded);

        // The expected output for "foobar" with line length 10 (rounded to 8) is:
        // "MZXW6YTBOI======\r\n"
        // Since the line length is larger than the encoded data length, the
        // separator appears only at the very end.
        assertTrue(encodedStr.endsWith("\r\n"), "Encoded output should end with the configured CRLF separator");
        assertEquals(EXPECTED_DEFAULT + "\r\n", encodedStr, "Chunked encoding should match the expected value");
    }

    /**
     * Verifies that the builder can be used to change the padding character and
     * that the resulting codec respects the new padding during encoding.
     */
    @Test
    void builderWithCustomPadding() {
        Base32 base32 = builder
                .setPadding('#')
                .get();

        assertNotNull(base32, "Builder should return a non‑null Base32 instance with custom padding");

        byte[] encoded = base32.encode(SAMPLE);
        String encodedStr = new String(encoded);

        // The default padding '=' is replaced by '#'
        assertFalse(encodedStr.contains("="), "Encoded output must not contain the default padding character");
        assertTrue(encodedStr.endsWith("######"), "Encoded output should end with the custom padding characters");
    }

    /**
     * Verifies that the builder can be instructed to use the Base32hex variant.
     */
    @Test
    void builderWithHexVariant() {
        Base32 base32 = builder
                .setUseHex(true)               // switch to the "base32hex" alphabet
                .get();

        assertNotNull(base32, "Builder should return a non‑null Base32 instance using the hex alphabet");

        // Encode the same sample using the hex alphabet and compare with a known value.
        // The hex alphabet encoding of "foobar" is "CPNMUOJ1E2======"
        byte[] encoded = base32.encode(SAMPLE);
        String encodedStr = new String(encoded);
        assertEquals("CPNMUOJ1E2======", encodedStr, "Hex variant encoding should match the RFC‑4648 Base32hex example");
    }

    /**
     * Verifies that a custom decoding policy can be set via the builder.
     * The test only checks that the builder accepts the policy without throwing.
     */
    @Test
    void builderWithDecodingPolicy() {
        Base32 base32 = builder
                .setDecodingPolicy(CodecPolicy.LENIENT) // the default, but we set it explicitly
                .get();

        assertNotNull(base32, "Builder should return a non‑null Base32 instance with a decoding policy");
        // No further behavioural check is needed here; the absence of an exception is enough.
    }
}
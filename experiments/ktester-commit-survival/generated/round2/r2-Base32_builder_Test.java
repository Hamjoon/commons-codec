package org.apache.commons.codec.binary;

import org.apache.commons.codec.CodecPolicy;
import org.junit.jupiter.api.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class Base32_builder_Test {

    /**
     * The builder used in the tests – created fresh for each test.
     */
    private Base32.Builder builder;

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a fresh builder before each test.
        builder = Base32.builder();
    }

    /**
     * Verify that the default builder creates a {@link Base32} instance with the
     * standard configuration (no line length, CRLF separator, '=' padding and
     * lenient decoding policy). The encoding of a known string is compared with
     * the RFC‑4648 reference output.
     */
    @Test
    void testBuilderCreatesDefaultBase32() {
        // default configuration
        Base32 base32 = builder.get();
        assertNotNull(base32, "Builder should return a non‑null Base32 instance");
        // Encode the word "foo" – RFC‑4648 expects "MZXW6===" for the default Base32.
        byte[] encoded = base32.encode("foo".getBytes(StandardCharsets.UTF_8));
        String encodedStr = new String(encoded, StandardCharsets.US_ASCII);
        assertEquals("MZXW6===", encodedStr, "Default Base32 encoding does not match RFC‑4648");
    }

    /**
     * Verify that a custom configuration supplied via the builder is honoured.
     * The test sets a line length, a line separator, a custom padding character
     * and a strict decoding policy, then checks that the resulting encoded data
     * reflects those settings.
     */
    @Test
    void testBuilderCustomConfiguration() {
        // Custom configuration: line length 8, CRLF separator, '*' padding, strict policy.
        Base32 customBase32 = Base32.builder().setLineLength(8).setLineSeparator('\r', '\n').setPadding((byte) '*').setDecodingPolicy(CodecPolicy.STRICT).get();
        assertNotNull(customBase32, "Custom builder should produce a Base32 instance");
        // Encode a single byte "f". With lineLength=8 the output should be exactly 8
        // encoded characters followed by the line separator. Padding must be '*'.
        byte[] encoded = customBase32.encode("f".getBytes(StandardCharsets.UTF_8));
        String encodedStr = new String(encoded, StandardCharsets.US_ASCII);
        // Expected 8 characters (including padding) + CRLF.
        assertTrue(encodedStr.endsWith("\r\n"), "Encoded output should end with the configured line separator");
        // Remove the line separator to examine the core Base32 payload.
        String payload = encodedStr.substring(0, encodedStr.length() - 2);
        assertEquals(8, payload.length(), "Payload length should match the configured line length");
        // The last six characters are padding; they must be '*'.
        assertTrue(payload.endsWith("******"), "Padding characters should be the custom '*' character");
    }

    /**
     * Ensure that each call to {@code Base32.builder()} returns a new independent
     * builder instance. Mutating one builder must not affect another.
     */
    @Test
    void testBuilderInstancesAreIndependent() {
        Base32.Builder first = Base32.builder().setLineLength(4);
        // no configuration
        Base32.Builder second = Base32.builder();
        Base32 firstBase32 = first.get();
        Base32 secondBase32 = second.get();
        // The first instance should have a line length of 4 (i.e., a line separator will be inserted).
        // The second instance uses the default (no line length).
        byte[] firstEncoded = firstBase32.encode("f".getBytes(StandardCharsets.UTF_8));
        byte[] secondEncoded = secondBase32.encode("f".getBytes(StandardCharsets.UTF_8));
        String firstStr = new String(firstEncoded, StandardCharsets.US_ASCII);
        String secondStr = new String(secondEncoded, StandardCharsets.US_ASCII);
        // The first encoding must contain a line separator (CRLF) because of lineLength=4.
        assertTrue(firstStr.contains("\r\n"), "First builder should have inserted a line separator due to lineLength=4");
        // The second encoding must *not* contain a line separator.
        assertFalse(secondStr.contains("\r\n"), "Second builder should use the default configuration with no line separator");
    }

    /**
     * Verify that providing a custom encode table works without throwing an
     * exception and that the resulting Base32 instance can still encode data.
     * The exact output is not checked – only that encoding succeeds.
     */
    @Test
    void testBuilderWithCustomEncodeTable() {
        // A simple reversible custom table (shifted alphabet).
        byte[] customTable = new byte[32];
        for (int i = 0; i < 32; i++) {
            customTable[i] = (byte) ('A' + ((i + 1) % 32));
        }
        Base32 customBase32 = Base32.builder().setEncodeTable(customTable).get();
        assertNotNull(customBase32, "Builder with custom encode table should produce a Base32 instance");
        // Encode some data – we only assert that no exception is thrown and that a result is returned.
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        byte[] encoded = customBase32.encode(data);
        assertNotNull(encoded, "Encoded result should not be null");
        assertTrue(encoded.length > 0, "Encoded result should contain data");
    }

    /**
     * JSON case: Verify that calling {@code Base32.builder()} never returns {@code null}.
     */
    @Test
    void testBuilderNeverReturnsNull() {
        Base32.Builder b = Base32.builder();
        assertNotNull(b, "Base32.builder() must never return null");
    }

    /**
     * JSON case: Two successive calls to {@code Base32.builder()} must return
     * distinct objects (different references).
     */
    @Test
    void testBuilderReturnsDistinctInstances() {
        Base32.Builder first = Base32.builder();
        Base32.Builder second = Base32.builder();
        assertNotSame(first, second, "Each call to Base32.builder() should return a new instance");
    }

    /**
     * JSON case: The default {@link Base32} produced by {@code Builder#get()}
     * must use the standard RFC‑4648 alphabet.
     *
     * <p>We verify the alphabet by checking that every character defined by the
     * RFC is accepted by {@link Base32#isInAlphabet(byte)} and that a character
     * outside the alphabet (e.g., '@') is rejected.</p>
     */
    @Test
    void testBuilderProducesBase32WithStandardAlphabet() {
        Base32 base32 = Base32.builder().get();
        // RFC‑4648 Base32 alphabet: A‑Z and 2‑7.
        String rfcAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        for (char c : rfcAlphabet.toCharArray()) {
            assertTrue(base32.isInAlphabet((byte) c), "Standard alphabet character '" + c + "' should be recognised");
        }
        // A character that is not part of the alphabet must be rejected.
        assertFalse(base32.isInAlphabet((byte) '@'), "Character '@' must not be recognised as part of the alphabet");
    }

    /**
     * JSON case: Concurrent invocations of {@code Base32.builder()} must each obtain
     * an independent builder instance with no shared mutable state.
     *
     * <p>The test spawns a number of threads, each calling {@code Base32.builder()}
     * and then configuring the builder (setting a line length). After all threads
     * complete we verify that each builder retained its own configuration.</p>
     */
    @Test
    void testBuilderConcurrencyIndependence() throws InterruptedException, ExecutionException {
        final int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Base32.Builder>> futures = new ArrayList<>();
        // Each task waits on the latch so that all threads start building at (roughly) the same time.
        for (int i = 0; i < threadCount; i++) {
            // each thread uses a distinct line length
            final int lineLength = i;
            futures.add(executor.submit(() -> {
                // synchronize start
                startLatch.await();
                Base32.Builder b = Base32.builder().setLineLength(lineLength);
                return b;
            }));
        }
        // Release all threads simultaneously.
        startLatch.countDown();
        // Collect the builders and verify they are all distinct and retain their own configuration.
        List<Base32.Builder> builders = new ArrayList<>();
        for (Future<Base32.Builder> f : futures) {
            builders.add(f.get());
        }
        // Ensure all builder references are unique.
        long distinctCount = builders.stream().distinct().count();
        assertEquals(threadCount, distinctCount, "Each thread should obtain a distinct Builder instance");
        // Verify that the line length set in each builder is preserved.
        IntStream.range(0, threadCount).forEach(i -> {
            Base32.Builder b = builders.get(i);
            // The Builder class does not expose a getter for lineLength, but we can infer it
            // by building a Base32 instance and checking the encoded output length.
            Base32 codec = b.get();
            byte[] encoded = codec.encode("f".getBytes(StandardCharsets.UTF_8));
            String encodedStr = new String(encoded, StandardCharsets.US_ASCII);
            if (i == 0) {
                // lineLength == 0 means no line separator.
                assertFalse(encodedStr.contains("\r\n"), "Builder with lineLength=0 must not insert a line separator");
            } else {
                // Any non‑zero line length forces insertion of the default CRLF separator.
                assertTrue(encodedStr.contains("\r\n"), "Builder with lineLength=" + i + " must insert a line separator");
            }
        });
        executor.shutdownNow();
    }

    /**
     * JSON case: The builder must support fluent configuration (e.g., {@code withPadding(false)})
     * without throwing exceptions, and the resulting {@link Base32} instance must reflect
     * the configured options.
     *
     * <p>We test a subset of fluent methods that are publicly available:
     * {@code setPadding}, {@code setLineLength}, {@code setLineSeparator},
     * and {@code setDecodingPolicy}. After building, we verify that the
     * configuration influences the encoding output as expected.</p>
     */
    @Test
    void testBuilderFluentConfiguration() {
        // Chain several configuration calls fluently.
        Base32 codec = Base32.builder().setPadding(// disable padding by using NUL (treated as "no padding")
        (byte) 0).setLineLength(// force a line break after every 4 characters
        4).setLineSeparator(// use LF only
        '\n').setDecodingPolicy(CodecPolicy.STRICT).get();
        assertNotNull(codec, "Builder should produce a non‑null Base32 instance");
        // Encode a short string; with padding disabled we expect no trailing '=' characters.
        byte[] encoded = codec.encode("AB".getBytes(StandardCharsets.UTF_8));
        String encodedStr = new String(encoded, StandardCharsets.US_ASCII);
        // Verify that the line separator is LF and appears at the expected position.
        assertTrue(encodedStr.contains("\n"), "Encoded output should contain the configured LF line separator");
        // Verify that padding characters are absent.
        assertFalse(encodedStr.contains("="), "Padding should be disabled, so '=' must not appear in the output");
    }
}

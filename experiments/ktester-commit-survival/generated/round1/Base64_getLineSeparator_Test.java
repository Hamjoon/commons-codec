package org.apache.commons.codec.binary;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.apache.commons.codec.CodecPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Base64#getLineSeparator()}.
 *
 * <p>The tests cover the different constructors that affect the {@code lineSeparator}
 * field and verify that the method returns the expected value (including defensive copies).</p>
 */
@ExtendWith(MockitoExtension.class)
class Base64_getLineSeparator_Test{

    /* --------------------------------------------------------------------- */
    /*  Lifecycle hooks – they are currently empty but kept for symmetry   */
    /* --------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test resources required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- */
    /*  Tests                                                               */
    /* --------------------------------------------------------------------- */

    /**
     * The no‑arg constructor creates a codec with {@code lineLength = 0},
     * which disables chunking and therefore the line separator should be {@code null}.
     */
    @Test
    void getLineSeparator_defaultConstructor_returnsNull() {
        Base64 codec = new Base64();               // lineLength = 0
        assertNull(codec.getLineSeparator(),
                "Default constructor should have no line separator (null).");
    }

    /**
     * When a positive line length is supplied without an explicit separator,
     * the default CRLF separator is used. The returned array must contain the
     * two bytes {@code '\r'} and {@code '\n'}.
     */
    @Test
    void getLineSeparator_positiveLineLength_defaultSeparator() {
        int lineLength = 76;                       // typical MIME line length
        Base64 codec = new Base64(lineLength);    // uses CHUNK_SEPARATOR internally
        byte[] sep = codec.getLineSeparator();

        assertNotNull(sep, "Line separator should not be null for positive line length.");
        assertArrayEquals(new byte[] { '\r', '\n' }, sep,
                "Default line separator must be CRLF (\"\\r\\n\").");
    }

    /**
     * Verify that a custom line separator is cloned – mutating the original
     * array after construction must not affect the codec's internal state.
     */
    @Test
    void getLineSeparator_customSeparator_isDefensiveCopy() {
        int lineLength = 10;
        byte[] custom = new byte[] { '-', '_' }; // arbitrary separator
        Base64 codec = new Base64(lineLength, custom); // lineLength > 0

        // Mutate the original array after construction
        custom[0] = 'X';
        custom[1] = 'Y';

        byte[] sep = codec.getLineSeparator();
        assertNotNull(sep, "Separator should be present for positive line length.");
        assertArrayEquals(new byte[] { '-', '_' }, sep,
                "Codec must have made a defensive copy of the supplied separator.");
    }

    /**
     * When a positive line length is supplied together with a {@code null}
     * separator, chunking is disabled and the separator should be {@code null}.
     */
    @Test
    void getLineSeparator_positiveLineLength_nullSeparator() {
        Base64 codec = new Base64(20, null); // lineLength > 0, separator null
        assertNull(codec.getLineSeparator(),
                "Explicit null separator with positive line length must result in null.");
    }

    /**
     * The constructor must reject a line separator that contains Base64 alphabet
     * characters. The test supplies a separator that includes the character 'A'
     * (which is part of the Base64 alphabet) and expects an {@link IllegalArgumentException}.
     */
    @Test
    void constructor_lineSeparatorContainingBase64Chars_throwsException() {
        int lineLength = 20;
        byte[] badSeparator = new byte[] { 'A', 'B' }; // 'A' and 'B' are valid Base64 chars

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Base64(lineLength, badSeparator),
                "Separator containing Base64 alphabet characters must cause IAE.");

        // The message contains the offending separator string – a sanity check.
        assertTrue(ex.getMessage().contains("lineSeparator must not contain base64 characters"),
                "Exception message should mention the illegal line separator.");
    }

    /**
     * Verify that the {@code lineSeparator} is {@code null} when the codec is
     * constructed with a zero line length even if a non‑null separator is supplied.
     */
    @Test
    void getLineSeparator_zeroLineLength_ignoresProvidedSeparator() {
        byte[] custom = new byte[] { '*', '*' };
        Base64 codec = new Base64(0, custom); // lineLength = 0 forces no separator
        assertNull(codec.getLineSeparator(),
                "Zero line length must ignore any supplied line separator and return null.");
    }

    /**
     * Ensure that the codec created via the {@link Base64.Builder} respects the
     * line separator configuration. The builder is the public API for constructing
     * instances, so we also test that path.
     */
    @Test
    void builder_customLineSeparator_isApplied() {
        byte[] custom = new byte[] { '~', '~' };
        Base64 codec = Base64.builder()
                .setLineLength(30)
                .setLineSeparator((char) '~', (char) '~')
                .get();

        assertArrayEquals(custom, codec.getLineSeparator(),
                "Builder should configure the custom line separator correctly.");
    }

    /**
     * Confirm that the codec created with {@code urlSafe = true} does not affect
     * the line separator handling. The separator should still be present when
     * a positive line length is used.
     */
    @Test
    void urlSafeFlag_doesNotImpactLineSeparator() {
        Base64 codec = new Base64(40, new byte[] { 'x', 'y' }, true);
        assertArrayEquals(new byte[] { 'x', 'y' }, codec.getLineSeparator(),
                "URL‑safe flag must not change the line separator behaviour.");
    }

    /**
     * Verify that the codec respects the {@link CodecPolicy} constructor overload
     * and still returns the expected line separator.
     */
    @Test
    void constructor_withCodecPolicy_preservesLineSeparator() {
        Base64 codec = new Base64(40, new byte[] { '1', '2' }, false, CodecPolicy.STRICT);
        assertArrayEquals(new byte[] { '1', '2' }, codec.getLineSeparator(),
                "Line separator must be preserved when a CodecPolicy is supplied.");
    }
}
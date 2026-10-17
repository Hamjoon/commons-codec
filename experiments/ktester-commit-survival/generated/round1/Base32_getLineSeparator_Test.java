package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class Base32_getLineSeparator_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * The default constructor creates a codec with lineLength == 0,
     * therefore the internal lineSeparator must be {@code null}.
     */
    @Test
    void defaultConstructor_hasNoLineSeparator() {
        Base32 codec = new Base32();
        assertNull(codec.getLineSeparator(),
                "Default Base32 instance should have a null lineSeparator");
    }

    /**
     * When a positive lineLength and a non‑null separator are supplied,
     * the separator is stored (as a defensive copy).
     */
    @Test
    void customLineSeparator_isStoredAndCloned() {
        byte[] original = new byte[] {'\r', '\n'};
        Base32 codec = new Base32(10, original);

        // The returned array must equal the original content
        assertArrayEquals(original, codec.getLineSeparator(),
                "Returned lineSeparator should match the supplied one");

        // Modifying the original array must not affect the internal state
        original[0] = 'X';
        assertNotEquals(original[0], codec.getLineSeparator()[0],
                "Internal lineSeparator must be a defensive copy");
    }

    /**
     * Supplying a lineLength of 0 ignores any provided separator and results
     * in a {@code null} lineSeparator.
     */
    @Test
    void zeroLineLengthIgnoresProvidedSeparator() {
        byte[] sep = new byte[] {'\r', '\n'};
        Base32 codec = new Base32(0, sep);
        assertNull(codec.getLineSeparator(),
                "When lineLength is 0, lineSeparator must be null even if supplied");
    }

    /**
     * A positive lineLength requires a non‑null separator.
     * Passing {@code null} should raise an {@link IllegalArgumentException}.
     */
    @Test
    void positiveLineLengthNullSeparator_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Base32(5, null),
                "lineLength > 0 with null lineSeparator must throw IllegalArgumentException");
    }

    /**
     * The line separator must not contain characters that belong to the Base32
     * alphabet. Supplying such a separator must raise an {@link IllegalArgumentException}.
     */
    @Test
    void lineSeparatorContainingAlphabetChar_throws() {
        // 'A' is part of the Base32 alphabet
        byte[] badSeparator = new byte[] {'A', '\n'};
        assertThrows(IllegalArgumentException.class,
                () -> new Base32(5, badSeparator),
                "lineSeparator containing Base32 alphabet characters must throw IllegalArgumentException");
    }
}
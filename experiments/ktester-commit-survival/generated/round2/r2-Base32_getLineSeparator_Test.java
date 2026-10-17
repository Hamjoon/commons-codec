package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Base32_getLineSeparator_Test {

    /**
     * Instance used by tests that need a default‑constructed Base32.
     */
    private Base32 defaultBase32;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a Base32 with the no‑arg constructor for convenience.
        defaultBase32 = new Base32();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this class.
        defaultBase32 = null;
    }

    static Stream<Arguments> defaultConstructionProvider() {
        return Stream.of(Arguments.of("Default constructor", new Base32()), Arguments.of("Builder without explicit line separator", Base32.builder().build()));
    }

    static Stream<Arguments> constructorSeparatorProvider() {
        return Stream.of(Arguments.of("CRLF separator", new byte[] { '\r', '\n' }, new Base32(0, new byte[] { '\r', '\n' }), new byte[] { 13, 10 }), Arguments.of("Three spaces", new byte[] { ' ', ' ', ' ' }, new Base32(0, new byte[] { ' ', ' ', ' ' }), new byte[] { 32, 32, 32 }));
    }

    static Stream<Arguments> builderSeparatorProvider() {
        return Stream.of(Arguments.of("Single LF", new byte[] { '\n' }, Base32.builder().withLineSeparator(new byte[] { '\n' }).build(), new byte[] { 10 }), Arguments.of("Two pipe characters", new byte[] { '|', '|' }, Base32.builder().withLineSeparator(new byte[] { '|', '|' }).build(), new byte[] { 124, 124 }));
    }

    @Test
    void testGetLineSeparator_DefaultConstructor_ReturnsNull() {
        // The default constructor creates a codec with no line separator.
        assertNull(defaultBase32.getLineSeparator(), "Default Base32 should have a null lineSeparator");
    }

    @Test
    void testGetLineSeparator_LineLengthZero_ReturnsNull() {
        Base32 codec = new Base32(0, null);
        assertNull(codec.getLineSeparator(), "When lineLength is 0 the lineSeparator must be null");
    }

    @Test
    void testGetLineSeparator_CustomSeparator_ReturnsClone() {
        byte[] separator = new byte[] { '\r', '\n' };
        Base32 codec = new Base32(10, separator);
        // The returned array must equal the supplied one.
        assertArrayEquals(separator, codec.getLineSeparator(), "Returned lineSeparator should match the supplied array");
        // It must be a defensive copy – not the same reference.
        assertNotSame(separator, codec.getLineSeparator(), "Returned lineSeparator should be a clone, not the original reference");
    }

    @Test
    void testGetLineSeparator_ModifyingOriginalDoesNotAffectCodec() {
        byte[] separator = new byte[] { 'A', 'B' };
        Base32 codec = new Base32(5, separator);
        // Change the original array after construction.
        separator[0] = 'Z';
        // The codec's internal copy must remain unchanged.
        byte[] internal = codec.getLineSeparator();
        assertEquals('A', internal[0], "Internal lineSeparator must not reflect modifications to the original array");
        assertEquals('B', internal[1], "Internal lineSeparator must retain the original second byte");
    }

    @Test
    void testGetLineSeparator_MultipleConstructorsBehaviour() {
        // Hex variant with default line separator (null because lineLength defaults to 0)
        Base32 hexCodec = new Base32(true);
        assertNull(hexCodec.getLineSeparator(), "Hex codec with default settings should have null lineSeparator");
        // Explicit line length with custom separator
        byte[] sep = new byte[] { '*', '*' };
        Base32 customCodec = new Base32(8, sep, false);
        assertArrayEquals(sep, customCodec.getLineSeparator(), "Custom codec should expose the provided line separator");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("defaultConstructionProvider")
    void testDefaultConstruction(String description, Base32 codec) {
        // The codec should have no line separator (null).
        assertNull(codec.getLineSeparator(), "Expected null lineSeparator for: " + description);
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("constructorSeparatorProvider")
    void testCustomLineSeparatorViaConstructor(String description, byte[] separator, Base32 codec, byte[] expected) {
        assertArrayEquals(expected, codec.getLineSeparator(), "Line separator mismatch for: " + description);
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("builderSeparatorProvider")
    void testCustomLineSeparatorViaBuilder(String description, byte[] separator, Base32 codec, byte[] expected) {
        assertArrayEquals(expected, codec.getLineSeparator(), "Line separator mismatch for: " + description);
    }

    @Test
    void testReferenceSemantics_ModifyReturnedArrayAffectsInternalState() {
        // ASCII 42
        byte[] original = new byte[] { '*', '*' };
        Base32 codec = new Base32(0, original);
        // Retrieve the internal array and modify it.
        byte[] retrieved = codec.getLineSeparator();
        // change first element to '*'
        retrieved[0] = 42;
        // A second call must reflect the mutation.
        assertArrayEquals(new byte[] { 42, 42 }, codec.getLineSeparator(), "Modification of the returned array should be reflected in subsequent calls");
    }

    @Test
    void testReferenceSemantics_ModifySingleByteSeparator() {
        // ASCII 33
        byte[] original = new byte[] { '!' };
        Base32 codec = new Base32(0, original);
        byte[] retrieved = codec.getLineSeparator();
        // '?' (ASCII 63)
        retrieved[0] = 63;
        assertArrayEquals(new byte[] { 63 }, codec.getLineSeparator(), "Single‑byte separator should reflect modification");
    }

    @Test
    void testConstructorDefensiveCopy_OriginalArrayModificationDoesNotAffectCodec() {
        byte[] separator = new byte[] { 'A', 'B' };
        Base32 codec = new Base32(0, separator);
        // Mutate the original array after construction.
        separator[0] = 'Z';
        // The codec's internal copy must remain unchanged.
        byte[] internal = codec.getLineSeparator();
        assertEquals('A', internal[0], "Internal lineSeparator must not reflect modifications to the original array");
        assertEquals('B', internal[1], "Internal lineSeparator must retain the original second byte");
    }

    @Test
    void testEmptyLineSeparatorArray() {
        Base32 codec = new Base32(0, new byte[0]);
        assertNotNull(codec.getLineSeparator(), "Empty separator should not be null");
        assertEquals(0, codec.getLineSeparator().length, "Expected an empty array for line separator");
    }
}

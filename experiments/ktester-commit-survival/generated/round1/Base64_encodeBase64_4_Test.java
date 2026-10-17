package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

/**
 * Unit tests for {@link Base64#encodeBase64(byte[], boolean, boolean, int)}.
 *
 * The tests cover:
 * <ul>
 *   <li>empty input handling</li>
 *   <li>chunked vs non‑chunked encoding</li>
 *   <li>URL‑safe flag</li>
 *   <li>maximum result size validation</li>
 *   <li>basic URL‑safe alphabet verification</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64_4_Test{

    private static final Random RANDOM = new Random();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
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
     * When the input array is empty (or {@code null}), the method should return the
     * same reference without performing any encoding.
     */
    @Test
    void encodeBase64_EmptyInput_ReturnsSameArray() {
        byte[] empty = new byte[0];
        byte[] result = Base64.encodeBase64(empty, false, false, Integer.MAX_VALUE);
        assertSame(empty, result, "Empty input must be returned unchanged");
    }

    @Test
    void encodeBase64_NullInput_ReturnsNull() {
        byte[] result = Base64.encodeBase64(null, false, false, Integer.MAX_VALUE);
        assertNull(result, "Null input must be returned as null");
    }

    /**
     * Verify that non‑chunked, non‑URL‑safe encoding produces the same output as the
     * convenience overload {@link Base64#encodeBase64(byte[])}.
     */
    @Test
    void encodeBase64_NonChunked_NonUrlSafe_MatchesDefault() {
        byte[] data = "Apache Commons Codec".getBytes();
        byte[] expected = Base64.encodeBase64(data); // default: no chunking, not URL‑safe
        byte[] actual = Base64.encodeBase64(data, false, false, Integer.MAX_VALUE);
        assertArrayEquals(expected, actual, "Encoding should match default behaviour");
    }

    /**
     * Verify that chunked, URL‑safe encoding matches the dedicated overload.
     */
    @Test
    void encodeBase64_Chunked_UrlSafe_MatchesOverload() {
        byte[] data = new byte[120]; // longer than one MIME line to trigger chunking
        RANDOM.nextBytes(data);
        byte[] expected = Base64.encodeBase64(data, true, true); // uses Integer.MAX_VALUE internally
        byte[] actual = Base64.encodeBase64(data, true, true, Integer.MAX_VALUE);
        assertArrayEquals(expected, actual, "Chunked URL‑safe encoding should match overload");
    }

    /**
     * The method must throw {@link IllegalArgumentException} when the calculated
     * encoded length exceeds {@code maxResultSize}.
     */
    @Test
    void encodeBase64_MaxResultSizeTooSmall_ThrowsException() {
        byte[] data = "exceed".getBytes(); // small but we will set maxResultSize too low
        // Compute the length that the method would produce using the same constructor logic.
        byte[] fullResult = Base64.encodeBase64(data, false, false, Integer.MAX_VALUE);
        int tooSmall = fullResult.length - 1; // one byte less than required
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Base64.encodeBase64(data, false, false, tooSmall));
        assertTrue(ex.getMessage().contains("Input array too big"),
                "Exception message should indicate size problem");
    }

    /**
     * When URL‑safe is enabled the output must use '-' and '_' instead of '+' and '/'.
     */
    @Test
    void encodeBase64_UrlSafe_UsesUrlSafeAlphabet() {
        byte[] data = "URLSafeTest".getBytes();
        byte[] result = Base64.encodeBase64(data, false, true, Integer.MAX_VALUE);
        // Convert to a string for easier inspection
        String encoded = new String(result);
        assertFalse(encoded.contains("+"), "URL‑safe output must not contain '+'");
        assertFalse(encoded.contains("/"), "URL‑safe output must not contain '/'");
        assertTrue(encoded.contains("-") || encoded.contains("_"),
                "URL‑safe output should contain '-' or '_' when appropriate");
    }
}
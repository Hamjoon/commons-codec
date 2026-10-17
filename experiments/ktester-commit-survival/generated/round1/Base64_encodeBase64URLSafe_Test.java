package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link Base64#encodeBase64URLSafe(byte[])}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Typical encoding of a non‑empty byte array (URL‑safe alphabet, no padding).</li>
 *   <li>Handling of {@code null} input – the method should return {@code null}.</li>
 *   <li>Handling of an empty byte array – the method should return an empty array.</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64URLSafe_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
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
     * Verify that a typical string is encoded using the URL‑safe Base64 alphabet
     * and that padding characters are omitted.
     */
    @Test
    void encodeBase64URLSafe_standardString() {
        // "Hello World" → standard Base64: "SGVsbG8gV29ybGQ="
        // URL‑safe version should be without the trailing '='.
        final byte[] input = "Hello World".getBytes(StandardCharsets.UTF_8);
        final byte[] expected = "SGVsbG8gV29ybGQ".getBytes(StandardCharsets.US_ASCII);

        final byte[] actual = Base64.encodeBase64URLSafe(input);

        assertArrayEquals(expected, actual,
                "URL‑safe Base64 encoding should match expected value without padding");
    }

    /**
     * Verify that {@code null} input yields {@code null} output.
     */
    @Test
    void encodeBase64URLSafe_nullInput() {
        assertNull(Base64.encodeBase64URLSafe(null),
                "Encoding a null byte array should return null");
    }

    /**
     * Verify that an empty byte array yields an empty byte array.
     */
    @Test
    void encodeBase64URLSafe_emptyInput() {
        final byte[] input = new byte[0];
        final byte[] actual = Base64.encodeBase64URLSafe(input);
        assertNotNull(actual, "Result should not be null for empty input");
        assertEquals(0, actual.length, "Result should be an empty array for empty input");
    }
}
package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Base64;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Base64_encodeBase64URLSafe_Test {

    private static final Random RANDOM = new Random();

    /**
     * Supplies test data for groups:
     * <ul>
     *   <li>no padding needed (multiple of 3 bytes)</li>
     *   <li>padding removed (1‑byte and 2‑byte inputs)</li>
     *   <li>plus/slash replacement</li>
     * </ul>
     */
    private static Stream<Arguments> encodeBase64URLSafeProvider() {
        return Stream.of(// group: encodeBase64URLSafe_noPaddingNeeded
        Arguments.of(new byte[] { 97, 110, 121, 32, 99, 97, 114, 110, 97, 108, 32, 112, 108, 101, 97, 115, 117, 114, 101, 46 }, "YW55IGNhcm5hbCBwbGVhc3VyZS4"), // group: encodeBase64URLSafe_paddingRemoved – single byte
        Arguments.of(new byte[] { 1 }, "AQ"), // group: encodeBase64URLSafe_paddingRemoved – two bytes
        Arguments.of(new byte[] { 1, 2 }, "AQI"), // group: encodeBase64URLSafe_plusSlashReplacement
        Arguments.of(new byte[] { (byte) 0xFB, (byte) 0xEF, (byte) 0xFF }, "--__"));
    }

    @Test
    @DisplayName("encodeBase64URLSafe returns null when input is null")
    void testEncodeBase64URLSafe_NullInput() {
        assertNull(Base64.encodeBase64URLSafe(null), "Expected null output for null input");
    }

    @Test
    @DisplayName("encodeBase64URLSafe returns empty array when input is empty")
    void testEncodeBase64URLSafe_EmptyInput() {
        byte[] empty = new byte[0];
        assertArrayEquals(empty, Base64.encodeBase64URLSafe(empty), "Empty input should yield empty output");
    }

    @Test
    @DisplayName("encodeBase64URLSafe matches java.util.Base64 URL encoder for a simple string")
    void testEncodeBase64URLSafe_SimpleString() {
        byte[] data = "foobar".getBytes(StandardCharsets.UTF_8);
        byte[] expected = Base64.getUrlEncoder().withoutPadding().encode(data);
        byte[] actual = Base64.encodeBase64URLSafe(data);
        assertArrayEquals(expected, actual, "URL‑safe encoding of 'foobar' does not match expected value");
    }

    @Test
    @DisplayName("encodeBase64URLSafe matches java.util.Base64 URL encoder for random data")
    void testEncodeBase64URLSafe_RandomData() {
        // Generate 100 random bytes
        byte[] data = new byte[100];
        RANDOM.nextBytes(data);
        byte[] expected = Base64.getUrlEncoder().withoutPadding().encode(data);
        byte[] actual = Base64.encodeBase64URLSafe(data);
        assertArrayEquals(expected, actual, "URL‑safe encoding of random data does not match java.util.Base64 output");
    }

    @Test
    @DisplayName("URL‑safe output differs from standard Base64 output")
    void testEncodeBase64URLSafe_DiffersFromStandard() {
        byte[] data = "Apache Commons Codec".getBytes(StandardCharsets.UTF_8);
        // standard (may contain '+' '/' and padding)
        byte[] standard = Base64.encodeBase64(data);
        // URL‑safe (no padding, '-' '_' instead)
        byte[] urlSafe = Base64.encodeBase64URLSafe(data);
        // Verify that the two results are not equal (they should differ in at least one byte)
        assertFalse(java.util.Arrays.equals(standard, urlSafe), "URL‑safe output should differ from standard Base64 output");
        // Additionally, ensure that the URL‑safe output contains only URL‑safe characters
        String urlSafeStr = new String(urlSafe, StandardCharsets.US_ASCII);
        assertTrue(urlSafeStr.matches("^[A-Za-z0-9\\-\\_]*$"), "URL‑safe output contains illegal characters");
        // Ensure padding character '=' is not present
        assertFalse(urlSafeStr.contains("="), "URL‑safe output must not contain padding '='");
    }

    @ParameterizedTest(name = "[{index}] encodeBase64URLSafe({0}) → \"{1}\"")
    @MethodSource("encodeBase64URLSafeProvider")
    @DisplayName("encodeBase64URLSafe matches expected URL‑safe strings for edge cases")
    void testEncodeBase64URLSafe_EdgeCases(byte[] input, String expectedBase64UrlSafe) {
        byte[] actual = Base64.encodeBase64URLSafe(input);
        byte[] expected = expectedBase64UrlSafe.getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, actual, () -> "URL‑safe encoding mismatch. Expected: " + expectedBase64UrlSafe);
    }
}

package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class Base64_encodeBase64_3_Test{

    /** Sample data longer than the default line length (76) to test chunking. */
    private static final byte[] LONG_INPUT = new byte[200];

    /** Sample data that contains bytes which map to '+' and '/' in the standard Base64 alphabet. */
    private static final byte[] URL_SAFE_INPUT = new byte[] {
            (byte) 0xfb, (byte) 0xef, (byte) 0xff, (byte) 0x80, // encodes to "+/+" in standard
            (byte) 0x00, (byte) 0x01, (byte) 0x02               // extra bytes to avoid padding only
    };

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise LONG_INPUT with a predictable pattern.
        for (int i = 0; i < LONG_INPUT.length; i++) {
            LONG_INPUT[i] = (byte) i;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void encodeBase64_nullInput_returnsNull() {
        assertNull(Base64.encodeBase64(null, false, false),
                "Encoding a null byte array should return null");
    }

    @Test
    void encodeBase64_noChunk_noUrlSafe_standardAlphabet() {
        byte[] data = "Hello World".getBytes();
        byte[] expected = java.util.Base64.getEncoder().encode(data);
        byte[] actual = Base64.encodeBase64(data, false, false);
        assertArrayEquals(expected, actual, "Standard Base64 encoding should match java.util.Base64");
    }

    @Test
    void encodeBase64_chunked_insertsCRLFEvery76Chars() {
        // Encode a long byte array with chunking enabled.
        byte[] encoded = Base64.encodeBase64(LONG_INPUT, true, false);

        // The default line separator for Apache Commons Codec is CRLF.
        final String encodedStr = new String(encoded);
        // Verify that the encoded string contains CRLF and that the line length is 76 characters.
        String[] lines = encodedStr.split("\r\n");
        // All lines except possibly the last must be exactly 76 characters.
        for (int i = 0; i < lines.length - 1; i++) {
            assertEquals(76, lines[i].length(),
                    "Each chunked line should be exactly 76 characters long");
        }
        // Re‑encode using java.util.Base64 with MIME encoder (which also inserts CRLF) and compare.
        String javaEncoded = java.util.Base64.getMimeEncoder().encodeToString(LONG_INPUT);
        assertEquals(javaEncoded, encodedStr,
                "Chunked encoding should be identical to java.util.Base64 MIME encoder");
    }

    @Test
    void encodeBase64_urlSafe_replacesPlusSlashWithMinusUnderscore() {
        byte[] encodedStandard = Base64.encodeBase64(URL_SAFE_INPUT, false, false);
        byte[] encodedUrlSafe = Base64.encodeBase64(URL_SAFE_INPUT, false, true);

        // Convert to String for easier inspection.
        String std = new String(encodedStandard);
        String url = new String(encodedUrlSafe);

        // In the standard alphabet '+' and '/' appear; in URL‑safe they become '-' and '_' respectively.
        assertTrue(std.contains("+") || std.contains("/"),
                "Standard encoding should contain '+' or '/' characters");
        assertFalse(url.contains("+") || url.contains("/"),
                "URL‑safe encoding must not contain '+' or '/' characters");
        assertTrue(url.contains("-") || url.contains("_"),
                "URL‑safe encoding should contain '-' or '_' characters");

        // Verify that decoding the URL‑safe output using the generic decodeBase64 returns the original bytes.
        byte[] roundTrip = Base64.decodeBase64(encodedUrlSafe);
        assertArrayEquals(URL_SAFE_INPUT, roundTrip,
                "Decoding URL‑safe output should yield the original input");
    }
}
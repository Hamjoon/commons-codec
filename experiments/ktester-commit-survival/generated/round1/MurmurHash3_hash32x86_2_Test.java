package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class MurmurHash3_hash32x86_2_Test{

    /** 256 bytes in the range [0,255] arranged in random order – same data as in the main test suite. */
    private static final byte[] RANDOM_BYTES = new byte[] {
            46,  -10, -7, -72, -9,  84,  99, -112,  62,  77, -61, -36,  92,  20, -106, -97,
            38,  40, -132, -4, -71,  28,  63,  13, -43, -84,  85, -58, 118,  74, 109, -99,
           -124, -40,  76, -79, -83,  23, -116,  86, -110,  95,  54, -80, 114, -77, -22,
           -82, 141, 122,  12,  60, 116, -56, -114,   6, -89,  59, -16,  33,  29, -91,
           111, -13,  30, -37, 110, -1,  53,  32,  35,  64, -31,  96, -104,  70,  41,
           -123,  80, -127, 127,  57, -57,   5, -92, 151,  49,  26, -76, -53,  83, 108,
            39, -130, -48,  42, -50, 178,  19,  69, -33,  71, -25, -106, 125, -45, -24,
           -67, 211, -24, -67,  55,  44,  82,  48, -35,  43, -64, -15, 103, -101,  27,
            51, -93,  21, -87,  91,  94, -39, -65,  78,  72,  93, 102, 104, 105,   8,
           113, 100, -113,  89, -11,  227, 120, -96, 160, -103, 153, -111,  45, -38,
           -88, 168, -23, -27,  67,  22, -74,  98, -119, -128, -121,  11, -42,  66,  73,
           -85, -68, -86, 131, -49,  79, 106,  24,  75, -19, -62,   7, -127, -31,  81,
          -8, -14,  16,  25, -120, -109,  97,  52,  10, -75,  17, -51, 205,  58, 101,
            68, -26,   1,  37,   0, -34,  88, -126, 130, -108, -32,  47,  50, -59,  34,
           -44, 196, 209,  14,  36, -117, -28, 154,  31, -81, -54, -20, 161,   3, -94,
          -66, -46, -72, -2, -122, -92,   4,  61,  65, 117, -70, 107, -52,   9, -69,
          201,  90, -107, -30,  56, -17, -18, -21, 112,  87,  18, 121, 115, -118, 123,
          -46, -46,   2, -63, -90, -90, -98, -98, -122, -122, -126, -126, -112, -112,
          -108, -108, -104, -104, -100, -100, -96, -96, -92, -92, -88, -88, -84, -84,
          -80, -80, -76, -76, -72, -72, -68, -68, -64, -64, -60, -60, -56, -56, -52,
          -52, -48, -48, -44, -44, -40, -40, -36, -36, -32, -32, -28, -28, -24, -24,
          -20, -20, -16, -16, -12, -12,  -8,  -8,  -4,  -4,   0,   0,   4,   4,   8,
            8,  12,  12,  16,  16,  20,  20,  24,  24,  28,  28,  32,  32,  36,  36,
            40,  40,  44,  44,  48,  48,  52,  52,  56,  56,  60,  60,  64,  64,  68,
            68,  72,  72,  76,  76,  80,  80,  84,  84,  88,  88,  92,  92,  96,  96,
           100, 100, 104, 104, 108, 108, 112, 112, 116, 116, 120, 120, 124, 124,
           128, 128, 132, 132, 136, 136, 140, 140, 144, 144, 148, 148, 152, 152,
           156, 156, 160, 160, 164, 164, 168, 168, 172, 172, 176, 176, 180, 180,
           184, 184, 188, 188, 192, 192, 196, 196, 200, 200, 204, 204, 208, 208,
           212, 212, 216, 216, 220, 220, 224, 224, 228, 228, 232, 232, 236, 236,
           240, 240, 244, 244, 248, 248, 252, 252, -128, -124, -120, -116, -112,
           -108, -104, -100, -96, -92, -88, -84, -80, -76, -72, -68, -64, -60, -56,
           -52, -48, -44, -40, -36, -32, -28, -24, -20, -16, -12,  -8,  -4,   0,
            4,   8,  12,  16,  20,  24,  28,  32,  36,  40,  44,  48,  52,  56,  60,
            64,  68,  72,  76,  80,  84,  88,  92,  96, 100, 104, 108, 112, 116,
           120, 124, 128, 132, 136, 140, 144, 148, 152, 156, 160, 164, 168, 172,
           176, 180, 184, 188, 192, 196, 200, 204, 208, 212, 216, 220, 224, 228,
           232, 236, 240, 244, 248, 252
    };

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /** Helper that computes the hash using the incremental API – this should be identical to the static method. */
    private int incrementalHash32x86(final byte[] data, final int offset, final int length, final int seed) {
        final MurmurHash3.IncrementalHash32x86 inc = new MurmurHash3.IncrementalHash32x86();
        inc.start(seed);
        inc.add(data, offset, length);
        return inc.getValue();
    }

    @Test
    void hash32x86EmptyArray() {
        final byte[] empty = new byte[0];
        final int hashViaStatic = MurmurHash3.hash32x86(empty, 0, 0, 0);
        final int hashViaConvenience = MurmurHash3.hash32x86(empty);
        assertEquals(hashViaConvenience, hashViaStatic,
                "hash32x86(empty) should delegate to the four‑argument overload with seed 0");
    }

    @Test
    void hash32x86FullArrayDefaultSeedMatchesFourArgumentVersion() {
        final int hashFull = MurmurHash3.hash32x86(RANDOM_BYTES, 0, RANDOM_BYTES.length, 0);
        final int hashConvenient = MurmurHash3.hash32x86(RANDOM_BYTES);
        assertEquals(hashConvenient, hashFull,
                "Convenience overload must produce the same result as the full‑argument version with seed 0");
    }

    @Test
    void hash32x86SubArrayMatchesIncrementalHash() {
        final int offset = 13;
        final int length = 27; // arbitrary non‑multiple‑of‑4 length to hit the tail logic
        final int seed = 12345;

        final int staticHash = MurmurHash3.hash32x86(RANDOM_BYTES, offset, length, seed);
        final int incrementalHash = incrementalHash32x86(RANDOM_BYTES, offset, length, seed);

        assertEquals(incrementalHash, staticHash,
                "Static hash32x86 for a sub‑array must match the result from IncrementalHash32x86");
    }

    @Test
    void hash32x86VariousSeedsAndLengthsAreConsistent() {
        final int[] seeds = {0, 1, 42, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        final int[] lengths = {1, 2, 3, 4, 7, 16, 31, 64, 127, 256};

        for (final int seed : seeds) {
            for (final int len : lengths) {
                if (len > RANDOM_BYTES.length) {
                    continue; // skip lengths that exceed the test data
                }
                final int staticHash = MurmurHash3.hash32x86(RANDOM_BYTES, 0, len, seed);
                final int incrementalHash = incrementalHash32x86(RANDOM_BYTES, 0, len, seed);
                assertEquals(staticHash, incrementalHash,
                        () -> String.format("Mismatch for seed=%d, length=%d", seed, len));
            }
        }
    }
}
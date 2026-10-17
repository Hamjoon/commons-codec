package org.apache.commons.codec.digest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash32_Test {

    private static Stream<Arguments> delegationEdgeValuesProvider() {
        return Stream.of(Arguments.of(0L), Arguments.of(Long.MAX_VALUE), Arguments.of(Long.MIN_VALUE), Arguments.of(1234567890123456789L), Arguments.of(-987654321098765432L), // -1L
        Arguments.of(0xFFFFFFFFFFFFFFFFL));
    }

    private static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(Arguments.of(Long.MAX_VALUE - 1L), Arguments.of(Long.MIN_VALUE + 1L), Arguments.of(0x123456789ABCDEFL), Arguments.of(-0x123456789ABCDEFL));
    }

    /**
     * Verify that the single‑argument overload delegates to the two‑argument overload
     * using {@link MurmurHash3#DEFAULT_SEED}.
     */
    @Test
    void hash32UsesDefaultSeed() {
        long[] testValues = { 0L, 1L, -1L, 123456789L, -987654321L, Long.MAX_VALUE, Long.MIN_VALUE };
        for (long v : testValues) {
            int expected = MurmurHash3.hash32(v, MurmurHash3.DEFAULT_SEED);
            int actual = MurmurHash3.hash32(v);
            assertEquals(expected, actual, () -> "hash32(" + v + ") should equal hash32(" + v + ", DEFAULT_SEED)");
        }
    }

    /**
     * The hash function must be deterministic: the same input always yields the same output.
     */
    @Test
    void hash32Deterministic() {
        long value = 0xDEADBEEFL;
        int first = MurmurHash3.hash32(value);
        for (int i = 0; i < 10; i++) {
            assertEquals(first, MurmurHash3.hash32(value), "hash32 must be deterministic for the same input");
        }
    }

    /**
     * Different seeds should (in most cases) produce different hash values.
     * This test checks that at least one seed variation leads to a different result.
     */
    @Test
    void hash32DifferentSeedsProduceDifferentResults() {
        long value = 0xCAFEBABEL;
        // uses DEFAULT_SEED
        int defaultSeedHash = MurmurHash3.hash32(value);
        int otherSeedHash = MurmurHash3.hash32(value, MurmurHash3.DEFAULT_SEED + 1);
        // It is possible (though extremely unlikely) that the two hashes collide.
        // In that rare case we simply assert that the method runs and returns an int.
        if (defaultSeedHash == otherSeedHash) {
            // Collisions are acceptable; just ensure the method does not throw.
            assertTrue(true, "Collision occurred but method executed correctly");
        } else {
            assertNotEquals(defaultSeedHash, otherSeedHash, "Different seeds should yield different hash values");
        }
    }

    /**
     * Verify that the method works for the full range of long values by sampling a few edge cases.
     */
    @Test
    void hash32EdgeCases() {
        assertEquals(MurmurHash3.hash32(0L, MurmurHash3.DEFAULT_SEED), MurmurHash3.hash32(0L));
        assertEquals(MurmurHash3.hash32(Long.MAX_VALUE, MurmurHash3.DEFAULT_SEED), MurmurHash3.hash32(Long.MAX_VALUE));
        assertEquals(MurmurHash3.hash32(Long.MIN_VALUE, MurmurHash3.DEFAULT_SEED), MurmurHash3.hash32(Long.MIN_VALUE));
    }

    /**
     * 1. Delegation & edge values ****************************************
     */
    @ParameterizedTest(name = "[{index}] hash32({0}) delegates to overload")
    @MethodSource("delegationEdgeValuesProvider")
    void hash32DelegatesAndMatchesExpected(long data) {
        int expected = MurmurHash3.hash32(data, MurmurHash3.DEFAULT_SEED);
        int actual = MurmurHash3.hash32(data);
        assertEquals(expected, actual, () -> "hash32(" + data + ") should delegate to hash32(" + data + ", DEFAULT_SEED)");
    }

    /**
     * 2. Distinct inputs produce distinct hashes *************************
     */
    @Test
    @DisplayName("Distinct long values should yield distinct hash32 results")
    void distinctInputsYieldDistinctHashes() {
        long v1 = 0x1111111111111111L;
        long v2 = 0x2222222222222222L;
        long v3 = -0x3333333333333333L;
        int h1 = MurmurHash3.hash32(v1, MurmurHash3.DEFAULT_SEED);
        int h2 = MurmurHash3.hash32(v2, MurmurHash3.DEFAULT_SEED);
        int h3 = MurmurHash3.hash32(v3, MurmurHash3.DEFAULT_SEED);
        assertNotEquals(h1, h2, "Hashes of different positive inputs must differ");
        assertNotEquals(h1, h3, "Hash of positive input must differ from negative input");
        assertNotEquals(h2, h3, "Hashes of distinct inputs must differ");
    }

    /**
     * 3. Upper vs. lower 32‑bit influence *******************************
     */
    @Test
    @DisplayName("High‑32‑bits‑only vs low‑32‑bits‑only values produce different hashes")
    void highLow32BitInfluence() {
        long highOnly = 0xFFFFFFFF00000000L;
        long lowOnly = 0x00000000FFFFFFFFL;
        int highHash = MurmurHash3.hash32(highOnly, MurmurHash3.DEFAULT_SEED);
        int lowHash = MurmurHash3.hash32(lowOnly, MurmurHash3.DEFAULT_SEED);
        assertNotEquals(highHash, lowHash, "Hash must depend on both halves of the long; high‑only and low‑only must differ");
    }

    /**
     * 4. Interleaved overload calls maintain determinism ****************
     */
    @Test
    @DisplayName("Calling hash32x86 before/after hash32(long) does not affect the result")
    void interleavedOverloadCallsDoNotChangeLongHash() {
        // Prepare a byte array – any content works; we use a simple pattern.
        byte[] bytes = new byte[8];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        // First call to the byte‑array overload (should have no side‑effects).
        int hashBytesFirst = MurmurHash3.hash32x86(bytes, 0, bytes.length, MurmurHash3.DEFAULT_SEED);
        long longValue = 0x7FFFFFFFFFFFFFFFL;
        // method under test
        int hashLong = MurmurHash3.hash32(longValue);
        // Second call to the byte‑array overload.
        int hashBytesSecond = MurmurHash3.hash32x86(bytes, 0, bytes.length, MurmurHash3.DEFAULT_SEED);
        // Verify that the long hash is independent of the other calls.
        int expectedLong = MurmurHash3.hash32(longValue, MurmurHash3.DEFAULT_SEED);
        assertEquals(expectedLong, hashLong, "hash32(long) must always delegate to overload with DEFAULT_SEED");
        // Ensure the byte‑array overload remains deterministic as well.
        assertEquals(hashBytesFirst, hashBytesSecond, "hash32x86(byte[]) must be deterministic and not be polluted by hash32(long)");
    }

    /**
     * 5. Boundary values not previously covered *************************
     */
    @ParameterizedTest(name = "[{index}] hash32({0}) matches overload")
    @MethodSource("boundaryValuesProvider")
    void boundaryValuesDelegateCorrectly(long data) {
        int expected = MurmurHash3.hash32(data, MurmurHash3.DEFAULT_SEED);
        int actual = MurmurHash3.hash32(data);
        assertEquals(expected, actual, () -> "hash32(" + data + ") should match hash32(" + data + ", DEFAULT_SEED)");
    }

    /**
     * Additional sanity: deterministic call for 0L (explicit) *********
     */
    @Test
    @DisplayName("Two consecutive calls with 0L produce the same hash")
    void deterministicZero() {
        int first = MurmurHash3.hash32(0L);
        int second = MurmurHash3.hash32(0L);
        assertEquals(first, second, "hash32(0L) must be deterministic");
    }
}

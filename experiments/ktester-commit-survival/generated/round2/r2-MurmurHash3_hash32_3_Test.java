package org.apache.commons.codec.digest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash32_3_Test {

    /**
     * Verify that the two‑argument overload returns the same value as the
     * three‑argument method when the default seed is supplied explicitly.
     */
    @Test
    void hash32_twoArgsEqualsThreeArgsWithDefaultSeed() {
        long data1 = 0x0123456789ABCDEFL;
        long data2 = 0xFEDCBA9876543210L;
        int expected = MurmurHash3.hash32(data1, data2, MurmurHash3.DEFAULT_SEED);
        int actual = MurmurHash3.hash32(data1, data2);
        assertEquals(expected, actual, "hash32(long,long) should delegate to hash32(long,long,int) with DEFAULT_SEED");
    }

    /**
     * Check a few deterministic input pairs, including zeroes and negative
     * values, against the three‑argument version.
     */
    @Test
    void hash32_knownValues() {
        // (data1, data2) -> expected hash (computed via the three‑arg method)
        long[][] inputs = { { 0L, 0L }, { 1L, 1L }, { -1L, -1L }, { Long.MAX_VALUE, Long.MIN_VALUE }, { 0x12345678ABCDEFL, 0x0FEDCBA987654321L } };
        for (long[] pair : inputs) {
            long d1 = pair[0];
            long d2 = pair[1];
            int expected = MurmurHash3.hash32(d1, d2, MurmurHash3.DEFAULT_SEED);
            int actual = MurmurHash3.hash32(d1, d2);
            assertEquals(expected, actual, () -> String.format("hash32(%#x, %#x) mismatch", d1, d2));
        }
    }

    /**
     * Ensure that the method behaves consistently when called repeatedly
     * with the same inputs (i.e., it is deterministic).
     */
    @Test
    void hash32_isDeterministic() {
        long data1 = 0xDEADBEEFL;
        long data2 = 0xCAFEBABEL;
        int first = MurmurHash3.hash32(data1, data2);
        for (int i = 0; i < 10; i++) {
            int subsequent = MurmurHash3.hash32(data1, data2);
            assertEquals(first, subsequent, "hash32 should return the same result on repeated invocations");
        }
    }

    /**
     * Verify that the method works with the full range of long values by
     * sampling a few random pairs. The test does not assert a specific value;
     * it only checks that the call does not throw and that the result is
     * consistent with the three‑argument overload.
     */
    @Test
    void hash32_randomSamples() {
        for (int i = 0; i < 20; i++) {
            long data1 = ThreadLocalRandom.current().nextLong();
            long data2 = ThreadLocalRandom.current().nextLong();
            int expected = MurmurHash3.hash32(data1, data2, MurmurHash3.DEFAULT_SEED);
            int actual = MurmurHash3.hash32(data1, data2);
            assertEquals(expected, actual, () -> String.format("Random sample %d failed for (%#x, %#x)", i, data1, data2));
        }
    }
}

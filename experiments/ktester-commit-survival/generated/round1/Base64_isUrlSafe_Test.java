package org.apache.commons.codec.binary;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Base64#isUrlSafe()}.
 *
 * <p>The tests verify that the {@code isUrlSafe} flag is correctly set by the
 * various constructors and by the {@link Base64.Builder}.</p>
 */
@ExtendWith(MockitoExtension.class)
class Base64_isUrlSafe_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    /**
     * The default constructor should create a non‑URL‑safe instance.
     */
    @Test
    void defaultConstructor_isNotUrlSafe() {
        Base64 codec = new Base64();
        assertFalse(codec.isUrlSafe(), "Default constructor must produce a non‑URL‑safe Base64");
    }

    /**
     * Constructor with explicit {@code urlSafe} flag.
     */
    @Test
    void booleanConstructor_behaviour() {
        Base64 safe = new Base64(true);
        Base64 unsafe = new Base64(false);

        assertTrue(safe.isUrlSafe(), "Constructor with true must be URL‑safe");
        assertFalse(unsafe.isUrlSafe(), "Constructor with false must not be URL‑safe");
    }

    /**
     * Constructor that specifies line length, line separator and URL‑safe flag.
     */
    @Test
    void lineLengthSeparatorConstructor_behaviour() {
        // lineLength = 0 (no chunking), lineSeparator = null
        Base64 safe = new Base64(0, null, true);
        Base64 unsafe = new Base64(0, null, false);

        assertTrue(safe.isUrlSafe(), "Line‑length/separator constructor with true must be URL‑safe");
        assertFalse(unsafe.isUrlSafe(), "Line‑length/separator constructor with false must not be URL‑safe");
    }

    /**
     * Builder should honour the {@code setUrlSafe} setting.
     */
    @Test
    void builderUrlSafeSetting() {
        Base64 safe = Base64.builder()
                .setUrlSafe(true)
                .get();
        Base64 unsafe = Base64.builder()
                .setUrlSafe(false)
                .get();

        assertTrue(safe.isUrlSafe(), "Builder setUrlSafe(true) must produce a URL‑safe instance");
        assertFalse(unsafe.isUrlSafe(), "Builder setUrlSafe(false) must produce a non‑URL‑safe instance");
    }

    /**
     * Builder without an explicit {@code setUrlSafe} call should default to non‑URL‑safe.
     */
    @Test
    void builderDefaultIsNotUrlSafe() {
        Base64 codec = Base64.builder().get();
        assertFalse(codec.isUrlSafe(), "Builder default must be non‑URL‑safe");
    }
}
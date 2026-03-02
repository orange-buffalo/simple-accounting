package io.kotest.matchers;

/**
 * Compatibility shim for json-unit-kotest 5.x compiled against kotest &lt; 6.1.4.
 * In kotest 6.1.4, AssertionCounter was moved from io.kotest.matchers to io.kotest.assertions.
 */
public interface AssertionCounter {
    int get();
    void reset();
    void inc();
}

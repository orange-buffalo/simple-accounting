package io.kotest.matchers;

/**
 * Compatibility shim for json-unit-kotest 5.x compiled against kotest &lt; 6.1.4.
 * In kotest 6.1.4, AssertionCounter_jvmKt was moved from io.kotest.matchers to io.kotest.assertions.
 */
public final class AssertionCounter_jvmKt {
    private AssertionCounter_jvmKt() {}

    public static AssertionCounter getAssertionCounter() {
        io.kotest.assertions.AssertionCounter delegate = AliasesKt.getAssertionCounter();
        return new AssertionCounter() {
            @Override
            public int get() {
                return delegate.get();
            }

            @Override
            public void reset() {
                delegate.reset();
            }

            @Override
            public void inc() {
                delegate.inc();
            }
        };
    }
}

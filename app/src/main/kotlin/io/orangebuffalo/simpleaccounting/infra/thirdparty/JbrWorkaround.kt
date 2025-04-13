package io.orangebuffalo.simpleaccounting.infra.thirdparty

/**
 * This annotation is to mark workarounds for issues with mocking libraries that
 * fail when running with JBR. There seems to be some issues
 * with byte code generation or similar that causes `StackOverflowError`. We could not
 * create a reproducible test case (only complex setup of our project engages the issue),
 * nor we found any similar reports. This happens for both mockito and mockk (although
 * the latter fails with explicit warning in byte-buddy processing code, while the former
 * just fails with `StackOverflowError`).
 *
 * To work around the issues, we have to use mocks instead of spies, and then declare interfaces
 * for the classes we want to spy on. This is not ideal, but it works.
 *
 * The purpose of this annotation is to track such workarounds in the codebase,
 * with some hope to address them in the future.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class JbrWorkaround

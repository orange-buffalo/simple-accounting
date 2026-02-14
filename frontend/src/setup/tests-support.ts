/**
 * A helper to determine if the application is running in a test environment.
 * When so, disabled animations and other non-essential features to speed up the
 * tests and make them more stable.
 */
export function isRunningInTest() {
  // @ts-expect-error
  return window.saRunningInTest === true;
}

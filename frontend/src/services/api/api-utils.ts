import { ApiBusinessError, ApiRequestCancelledError } from '@/services/api/api-errors.ts';

const DEFAULT_TIMEOUT_MS = 10000;

export function apiDateString(date: Date) {
  return `${date.getFullYear()}-${
    (`0${date.getMonth() + 1}`).slice(-2)}-${
    (`0${date.getDate()}`).slice(-2)}`;
}

/**
 * If a provided error is an API business error, returns its error code.
 * Otherwise, throws the error further.
 * This is a convenience function for GraphQL API business errors where the error code is the primary concern.
 */
export function handleGqlApiBusinessError<T extends string>(error: unknown): T {
  if (error instanceof ApiBusinessError) {
    return error.error.error as T;
  }
  throw error;
}

/**
 * Configuration for {@link #useRequestConfig}.
 */
export interface RequestConfigParams {
  /**
   * Custom timeout to override the defaults.
   */
  timeoutMs?: number;
}

/**
 * Return type of {@link #useRequestConfig}.
 */
export interface RequestConfigReturn {
  /**
   * To be passed into the API calls.
   */
  requestConfig: RequestInit;

  /**
   * Handle for manual request cancellation.
   */
  cancelRequest: () => void;
}

/**
 * Can be replaced with AbortSignal.any once it is widely adopted.
 */
function anyAbortSignal(...signals: AbortSignal[]) {
  const controller = new AbortController();
  signals.forEach((signal) => {
    signal.addEventListener('abort', function onAbord() {
      controller.abort(this.reason);
    }, { once: true });
  });
  return controller.signal;
}

/**
 * Allows to customize a {@link RequestInit} for API calls.
 */
export function useRequestConfig(params: RequestConfigParams): RequestConfigReturn {
  const abortController = new AbortController();
  return {
    requestConfig: {
      signal: anyAbortSignal(
        abortController.signal,
        AbortSignal.timeout(params.timeoutMs || DEFAULT_TIMEOUT_MS),
      ),
    },
    cancelRequest: () => abortController.abort(
      new ApiRequestCancelledError(),
    ),
  };
}

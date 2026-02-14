import { ApiBusinessError, ApiRequestCancelledError } from '@/services/api/api-errors.ts';
import type { ApiPage, ApiPageRequest, SaApiErrorDto } from '@/services/api/api-types';

const DEFAULT_TIMEOUT_MS = 10000;

let currentGlobalTimeoutMs = DEFAULT_TIMEOUT_MS;

/**
 * Updates the global request timeout. Only intended for one-time global setup, e.g. in tests.
 */
export function setGlobalRequestTimeout(requestTimeoutMs: number) {
  currentGlobalTimeoutMs = requestTimeoutMs;
}

export function getGlobalRequestTimeout() {
  return currentGlobalTimeoutMs;
}

export function apiDateString(date: Date) {
  return `${date.getFullYear()}-${(`0${date.getMonth() + 1}`).slice(-2)}-${(`0${date.getDate()}`).slice(-2)}`;
}

export async function consumeAllPages<T>(
  requestExecutor: (pageRequest: ApiPageRequest) => Promise<ApiPage<T>>,
): Promise<T[]> {
  let allPagesData: T[] = [];
  let pageNumber = 1;
  const pageSize = 100;
  let totalElements = 1000;
  while ((pageNumber - 1) * pageSize < totalElements) {
    const response = await requestExecutor({
      pageSize,
      pageNumber,
    });
    allPagesData = allPagesData.concat(response.data);
    pageNumber += 1;
    totalElements = response.totalElements;
  }
  return allPagesData;
}

/**
 * If a provided error is an API business error, returns its response body as desired type.
 * Otherwise, throws the error further.
 */
export function handleApiBusinessError<T extends SaApiErrorDto>(error: unknown): T {
  if (error instanceof ApiBusinessError) {
    return error.errorAs<T>();
  }
  throw error;
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
    signal.addEventListener(
      'abort',
      function onAbord() {
        controller.abort(this.reason);
      },
      { once: true },
    );
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
      signal: anyAbortSignal(abortController.signal, AbortSignal.timeout(params.timeoutMs || currentGlobalTimeoutMs)),
    },
    cancelRequest: () => abortController.abort(new ApiRequestCancelledError()),
  };
}

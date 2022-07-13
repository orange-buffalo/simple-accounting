import type { ApiPage, ApiPageRequest } from '@/services/api/api-types';
import { ResponseError } from '@/services/api/generated';
import type { AdditionalRequestParameters } from '@/services/api/generated/runtime';
import type { RequestMetadata } from '@/services/api/api-client';

export function apiDateString(date: Date) {
  return `${date.getFullYear()}-${
    (`0${date.getMonth() + 1}`).slice(-2)}-${
    (`0${date.getDate()}`).slice(-2)}`;
}

/**
 * @deprecated TODO: rewrite and cover with tests
 */
export async function consumeAllPages<T>(
  requestExecutor: (pageRequest: ApiPageRequest) => Promise<ApiPage<T>>,
): Promise<T[]> {
  let result: T[] = [];
  const request: ApiPageRequest = {
    pageNumber: 1,
    pageSize: 100,
  };
  let totalElements = 1000;
  // eslint-disable-next-line
  while ((request.pageNumber! - 1) * request.pageSize! < totalElements) {
    // eslint-disable-next-line no-await-in-loop
    const response = await requestExecutor(request);
    result = result.concat(response.data);
    // TODO
    // eslint-disable-next-line
    request.pageNumber! += 1;
    totalElements = response.totalElements;
  }
  return result;
}

/**
 * Consumes exception and returns the response body, if exception is caused by non-successful response.
 * @param e exception caught during API request execution.
 */
export async function consumeApiErrorResponse<T>(e: unknown): Promise<T | undefined> {
  if (e instanceof ResponseError) {
    return (await e.response.json()) as T;
  }
}

export function skipGlobalErrorHandler(): AdditionalRequestParameters<RequestMetadata> {
  return {
    metadata: {
      skipGlobalErrorHandler: true,
    },
  };
}

export function requestTimeout(timeoutMs: number): AdditionalRequestParameters<RequestMetadata> {
  return {
    metadata: {
      requestTimeoutMs: timeoutMs,
    },
  };
}

export interface CancellableRequest {
  cancellableRequestConfig: RequestInit;
  cancelRequest: () => void;
}

export function useCancellableRequest(): CancellableRequest {
  const abortController = new AbortController();
  return {
    cancellableRequestConfig: {
      signal: abortController.signal,
    },
    cancelRequest: () => abortController.abort(),
  };
}

export function defaultRequestSettings(): RequestInit {
  return {};
}

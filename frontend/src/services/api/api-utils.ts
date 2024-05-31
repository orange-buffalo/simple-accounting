import type { ApiPage, ApiPageRequest, SaApiErrorDto } from '@/services/api/api-types';
import type { AdditionalRequestParameters } from '@/services/api/generated/runtime';
import type { RequestMetadata } from '@/services/api/api-client';
import { ApiBusinessError, ApiRequestCancelledError } from '@/services/api/api-errors.ts';

export function apiDateString(date: Date) {
  return `${date.getFullYear()}-${
    (`0${date.getMonth() + 1}`).slice(-2)}-${
    (`0${date.getDate()}`).slice(-2)}`;
}

export async function consumeAllPages<T>(
  requestExecutor: (pageRequest: ApiPageRequest) => Promise<ApiPage<T>>,
): Promise<T[]> {
  let allPagesData: T[] = [];
  let pageNumber = 1;
  const pageSize = 100;
  let totalElements = 1000;
  while ((pageNumber - 1) * pageSize < totalElements) {
    // eslint-disable-next-line no-await-in-loop
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

export function requestTimeout(timeoutMs: number): AdditionalRequestParameters<RequestMetadata> {
  return {
    metadata: {
      requestTimeoutMs: timeoutMs,
    },
  };
}

export interface CancellableRequest {
  cancellableRequestConfig: RequestInit;
  cancelRequest: (reason?: unknown) => void;
}

export function useCancellableRequest(): CancellableRequest {
  const abortController = new AbortController();
  return {
    cancellableRequestConfig: {
      signal: abortController.signal,
    },
    cancelRequest: (reason?: unknown) => abortController.abort(
      new ApiRequestCancelledError(reason),
    ),
  };
}

export function defaultRequestSettings(): RequestInit {
  return {};
}

import type { ApiPage, ApiPageRequest } from '@/services/api/api-types';
import { ResponseError } from '@/services/api/generated';

export function apiDateString(date: Date) {
  return `${date.getFullYear()}-${
    (`0${date.getMonth() + 1}`).slice(-2)}-${
    (`0${date.getDate()}`).slice(-2)}`;
}

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

export async function consumeApiErrorResponse<T>(e: unknown): Promise<T> {
  if (!(e instanceof ResponseError)) throw new Error(`Unknown error ${JSON.stringify(e)}`);
  return (await e.response.json()) as T;
}

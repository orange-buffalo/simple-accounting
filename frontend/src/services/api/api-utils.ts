import { AxiosResponse } from 'axios';
import { ApiPage, ApiPageRequest } from '@/services/api/api-types';

export function apiDateString(date: Date) {
  return `${date.getFullYear()}-${
    (`0${date.getMonth() + 1}`).slice(-2)}-${
    (`0${date.getDate()}`).slice(-2)}`;
}

export async function consumeAllPages<T>(
  requestExecutor: (pageRequest: ApiPageRequest) => Promise<AxiosResponse<ApiPage<T>>>,
): Promise<T[]> {
  let result: T[] = [];
  const request: ApiPageRequest = {
    pageNumber: 1,
    pageSize: 100,
  };
  let totalElements = 1000;
  while ((request.pageNumber! - 1) * request.pageSize! < totalElements) {
    // eslint-disable-next-line no-await-in-loop
    const response = await requestExecutor(request);
    result = result.concat(response.data.data);
    request.pageNumber! += 1;
    totalElements = response.data.totalElements;
  }
  return result;
}

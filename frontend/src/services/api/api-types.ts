export * from '@/services/api/generated/models';
export type { AdditionalRequestParameters } from '@/services/api/generated/runtime';

export interface ApiPage<T> {
  pageNumber: number,
  totalElements: number,
  pageSize: number,
  data: Array<T>,
}

export interface ApiPageRequest {
  pageNumber?: number,
  pageSize?: number,
}

export interface HasOptionalId {
  id?: number,
}

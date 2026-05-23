import type { PageInfo } from '@/services/api/gql/schema-types.ts';

export interface ApiConnectionEdge<T> {
  cursor?: string | null,
  node: T,
}

export interface ApiConnection<T> {
  edges: Array<ApiConnectionEdge<T>>,
  pageInfo?: PageInfo,
  totalCount: number,
}

export interface ApiConnectionRequest {
  first?: number,
  after?: string | null,
}

export interface HasOptionalId {
  id?: string,
}

import { graphql } from '@/services/api/gql';
import type { FragmentType } from '@/services/api/gql/fragment-masking';

export const PaginationPageInfoFragment = graphql(`
  fragment PaginationPageInfo on PageInfo {
    endCursor
    hasNextPage
    hasPreviousPage
    startCursor
  }
`);

export type GqlConnection<N> = {
  edges: Array<{ node: N; cursor: string }>;
  pageInfo: FragmentType<typeof PaginationPageInfoFragment>;
  totalCount: number;
};

export type NodeOf<T> = T extends GqlConnection<infer N> ? N : unknown;

export type DeepAccess<T, P extends string> = P extends `${infer K}.${infer Rest}`
  ? K extends keyof T ? DeepAccess<T[K], Rest> : never
  : P extends keyof T ? T[P] : never;

export type GqlPaginationVariables = {
  first?: number | null;
  after?: string | null;
};

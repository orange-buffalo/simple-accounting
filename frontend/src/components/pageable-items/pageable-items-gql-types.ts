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

export type GqlPaginationVariables = {
  first: number;
  after?: string | null;
};

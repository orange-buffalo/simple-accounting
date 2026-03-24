import { graphql } from '@/services/api/gql';
import type { FragmentType } from '@/services/api/gql/fragment-masking';

export const PaginationInfoFragment = graphql(`
  fragment PaginationInfo on PaginatedConnection {
    pageInfo {
      endCursor
      hasNextPage
      hasPreviousPage
      startCursor
    }
    totalCount
  }
`);

export type GqlConnection<N> = {
  edges: Array<{ node: N; cursor: string }>;
} & FragmentType<typeof PaginationInfoFragment>;

export type GqlPaginationVariables = {
  first: number;
  after?: string | null;
};

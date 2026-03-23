export type GqlConnection<N> = {
  edges: Array<{ node: N; cursor: string }>;
  pageInfo: {
    endCursor?: string | null;
    hasNextPage: boolean;
    hasPreviousPage: boolean;
    startCursor?: string | null;
  };
  totalCount: number;
};

export type GqlPaginationVariables = {
  first: number;
  after?: string | null;
};

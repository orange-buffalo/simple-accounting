import { ApiBusinessError, ApiError } from '@/services/api/api-errors';
import { SaGrapQlErrorType } from '@/services/api/gql/graphql';
import type { TypedDocumentNode } from '@graphql-typed-document-node/core';
import { print } from 'graphql';

/**
 * Low-level GraphQL client that uses raw fetch to bypass the urql auth exchange.
 * Useful for operations that are part of the auth flow itself
 * (login, logout, token refresh) where using the auth-aware client
 * would create circular dependencies.
 */
export async function executeRawGqlMutation<
  TData,
  TVariables extends Record<string, unknown>,
>(
  mutation: TypedDocumentNode<TData, TVariables>,
  variables: TVariables,
): Promise<TData> {
  const response = await fetch('/api/graphql', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: print(mutation),
      variables,
    }),
  });

  const body = await response.json();
  if (body.errors?.length) {
    const graphQLError = body.errors[0];
    if (graphQLError.extensions?.errorType
      === SaGrapQlErrorType.BusinessError) {
      const businessError = new ApiBusinessError({
        error: graphQLError.extensions.errorCode,
        message: graphQLError.message,
      });
      Object.assign(businessError, graphQLError.extensions);
      throw businessError;
    }
    throw new ApiError(graphQLError.message || 'GraphQL request failed');
  }

  return body.data;
}

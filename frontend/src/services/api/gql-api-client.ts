import {
  Client, createClient, fetchExchange, provideClient, Operation, OperationResult,
} from '@urql/vue';
import type { Exchange } from '@urql/core';
import { pipe, tap, map } from 'wonka';
import { authExchange } from '@urql/exchange-auth';
import { getAuthorizationHeader, updateApiToken } from '@/services/api/auth';
import { LOADING_FINISHED_EVENT, LOADING_STARTED_EVENT, LOGIN_REQUIRED_EVENT } from '@/services/events';
import { getGlobalRequestTimeout } from '@/services/api';
import {
  ClientApiError,
} from '@/services/api/api-errors';

// Exchange to add authorization headers
const contextHeaderExchange: Exchange = ({ forward }) => (ops$) => {
  return pipe(
    ops$,
    map((operation: Operation) => {
      const authHeader = getAuthorizationHeader();
      if (authHeader) {
        const currentFetchOptions = operation.context.fetchOptions || {};
        const fetchOptions = typeof currentFetchOptions === 'function' 
          ? currentFetchOptions() 
          : currentFetchOptions;
          
        return {
          ...operation,
          context: {
            ...operation.context,
            fetchOptions: {
              ...fetchOptions,
              headers: {
                ...fetchOptions.headers,
                Authorization: authHeader,
              },
            },
          },
        };
      }
      return operation;
    }),
    forward,
  );
};

// Exchange to emit loading events
const loadingExchange: Exchange = ({ forward }) => (ops$) => {
  return pipe(
    ops$,
    tap(() => {
      LOADING_STARTED_EVENT.emit();
    }),
    forward,
    tap(() => {
      LOADING_FINISHED_EVENT.emit();
    }),
  );
};

// Exchange to handle errors and transform them to standard API errors  
const errorExchange: Exchange = ({ forward }) => (ops$) => {
  return pipe(
    ops$,
    forward,
    tap((result: OperationResult) => {
      if (result.error) {
        // Handle GraphQL errors (non-auth errors are thrown, auth errors are handled by authExchange)
        if (result.error.graphQLErrors && result.error.graphQLErrors.length > 0) {
          // Check if any error is NOT an auth error
          const hasNonAuthError = result.error.graphQLErrors.some(
            (error: any) => error.extensions?.errorType !== 'NOT_AUTHORIZED'
          );
          
          if (hasNonAuthError) {
            throw new ClientApiError('GraphQL error occurred', result.error);
          }
        }
        
        // Handle network errors  
        if (result.error.networkError) {
          throw new ClientApiError('Network error occurred', result.error);
        }
      }
    }),
  );
};

const client = createClient({
  url: '/api/graphql',
  fetchOptions: {
    signal: AbortSignal.timeout(getGlobalRequestTimeout()),
  },
  exchanges: [
    contextHeaderExchange,
    loadingExchange,
    authExchange(async (utils) => {
      let token = getAuthorizationHeader()?.replace('Bearer ', '') || null;
      
      return {
        addAuthToOperation(operation) {
          if (!token) return operation;
          
          return utils.appendHeaders(operation, {
            Authorization: `Bearer ${token}`,
          });
        },
        
        didAuthError(error) {
          return error.graphQLErrors?.some(
            (e: any) => e.extensions?.errorType === 'NOT_AUTHORIZED'
          ) || false;
        },
        
        async refreshAuth() {
          try {
            const refreshMutation = `
              mutation RefreshAccessToken {
                refreshAccessToken {
                  accessToken
                }
              }
            `;
            
            const result = await utils.mutate(refreshMutation, {});
            
            if (result.data?.refreshAccessToken?.accessToken) {
              token = result.data.refreshAccessToken.accessToken;
              updateApiToken(token);
            } else {
              // Refresh failed
              token = null;
              LOGIN_REQUIRED_EVENT.emit();
            }
          } catch (error) {
            // Refresh failed
            token = null;
            LOGIN_REQUIRED_EVENT.emit();
          }
        },
        
        willAuthError() {
          // We can't predict auth errors in GraphQL
          return false;
        },
      };
    }),
    errorExchange,
    fetchExchange,
  ],
});

export function setupGqlClient() {
  provideClient(client);
}

export { client as gqlClient };

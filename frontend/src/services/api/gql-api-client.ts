import {
  Client, createClient, fetchExchange, provideClient, Operation, OperationResult,
} from '@urql/vue';
import type { Exchange } from '@urql/core';
import { pipe, tap, map } from 'wonka';
import { getAuthorizationHeader, tryAutoLogin } from '@/services/api/auth';
import { LOADING_FINISHED_EVENT, LOADING_STARTED_EVENT, LOGIN_REQUIRED_EVENT } from '@/services/events';
import { getGlobalRequestTimeout } from '@/services/api';
import {
  ClientApiError,
} from '@/services/api/api-errors';

// Exchange to add authorization headers cleanly
const authExchange: Exchange = ({ forward }) => (ops$) => {
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

// Exchange to handle timeouts using fetchOptions
const timeoutExchange: Exchange = ({ forward }) => (ops$) => {
  return pipe(
    ops$,
    map((operation: Operation) => {
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
            signal: fetchOptions.signal || AbortSignal.timeout(getGlobalRequestTimeout()),
          },
        },
      };
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

// Exchange to handle authorization errors - for now just emit login event
// TODO: Implement proper retry logic with token refresh
const authRetryExchange: Exchange = ({ forward }) => (ops$) => {
  return pipe(
    ops$,
    forward,
    map((result: OperationResult) => {
      // Check for authorization errors in GraphQL response
      if (result.error?.graphQLErrors) {
        const hasAuthError = result.error.graphQLErrors.some(
          (error: any) => error.extensions?.errorType === 'NOT_AUTHORIZED'
        );
        
        if (hasAuthError) {
          // For now, try auto login in background and emit login event
          tryAutoLogin().then((refreshSuccessful) => {
            if (!refreshSuccessful) {
              LOGIN_REQUIRED_EVENT.emit();
            }
            // TODO: Implement proper retry mechanism
          }).catch(() => {
            LOGIN_REQUIRED_EVENT.emit();
          });
        }
      }
      
      return result;
    }),
  );
};

// Exchange to handle errors and transform them to standard API errors  
const errorExchange: Exchange = ({ forward }) => (ops$) => {
  return pipe(
    ops$,
    forward,
    map((result: OperationResult) => {
      if (result.error) {
        // Handle non-auth GraphQL errors only
        if (result.error.graphQLErrors) {
          const hasAuthError = result.error.graphQLErrors.some(
            (error: any) => error.extensions?.errorType === 'NOT_AUTHORIZED'
          );
          
          // Only throw for non-auth errors since auth errors are handled by authRetryExchange
          if (!hasAuthError) {
            throw new ClientApiError('GraphQL error occurred', result.error);
          }
        }
        
        // Handle network errors  
        if (result.error.networkError) {
          throw new ClientApiError('Network error occurred', result.error);
        }
      }
      
      return result;
    }),
  );
};

const client = createClient({
  url: '/api/graphql',
  exchanges: [
    timeoutExchange,
    authExchange,
    loadingExchange,
    authRetryExchange,
    errorExchange,
    fetchExchange,
  ],
});

export function setupGqlClient() {
  provideClient(client);
}

export { client as gqlClient };

import {
  Client, createClient, fetchExchange, provideClient, Operation, OperationResult,
} from '@urql/vue';
import type { Exchange } from '@urql/core';
import { pipe, tap, map } from 'wonka';
import { getAuthorizationHeader } from '@/services/api/auth';
import { LOADING_FINISHED_EVENT, LOADING_STARTED_EVENT, LOGIN_REQUIRED_EVENT } from '@/services/events';
import { getGlobalRequestTimeout } from '@/services/api';
import {
  ApiAuthError,
  ApiRequestCancelledError,
  ApiTimeoutError,
  ClientApiError,
} from '@/services/api/api-errors';

// Exchange to add authorization headers
const authExchange: Exchange = ({ forward }) => (ops$) => {
  return pipe(
    ops$,
    map((operation: Operation) => {
      const authHeader = getAuthorizationHeader();
      if (authHeader) {
        const currentOptions = typeof operation.context.fetchOptions === 'function' 
          ? operation.context.fetchOptions() 
          : operation.context.fetchOptions || {};
          
        return {
          ...operation,
          context: {
            ...operation.context,
            fetchOptions: {
              ...currentOptions,
              headers: {
                ...currentOptions.headers,
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

// Exchange to handle timeouts
const timeoutExchange: Exchange = ({ forward }) => (ops$) => {
  return pipe(
    ops$,
    map((operation: Operation) => {
      const currentOptions = typeof operation.context.fetchOptions === 'function'
        ? operation.context.fetchOptions()
        : operation.context.fetchOptions || {};
        
      if (!currentOptions.signal) {
        return {
          ...operation,
          context: {
            ...operation.context,
            fetchOptions: {
              ...currentOptions,
              signal: AbortSignal.timeout(getGlobalRequestTimeout()),
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

// Exchange to handle authorization errors and token refresh
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
          // For GraphQL, we'll handle token refresh outside the exchange
          // and let the application layer retry if needed
          LOGIN_REQUIRED_EVENT.emit();
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
        // Handle authorization errors - convert to consistent error structure
        if (result.error.graphQLErrors) {
          const hasAuthError = result.error.graphQLErrors.some(
            (error: any) => error.extensions?.errorType === 'NOT_AUTHORIZED'
          );
          
          if (hasAuthError) {
            // Don't throw here - URQL will handle the error in the result
            // Just ensure the LOGIN_REQUIRED_EVENT gets emitted in authRetryExchange
            return result;
          }
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

// Helper function to attempt token refresh using GraphQL
export async function refreshAccessToken(): Promise<string | null> {
  try {
    const query = `
      mutation RefreshAccessToken {
        refreshAccessToken {
          accessToken
        }
      }
    `;
    
    const response = await client.mutation(query, {}).toPromise();
    
    if (response.data?.refreshAccessToken?.accessToken) {
      return response.data.refreshAccessToken.accessToken;
    }
    
    return null;
  } catch (error) {
    return null;
  }
}

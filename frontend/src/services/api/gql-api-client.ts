import { Client, fetchExchange, provideClient } from '@urql/vue';

import type { Exchange } from '@urql/core';
import { LOADING_FINISHED_EVENT, LOADING_STARTED_EVENT } from '@/services/events';
import { graphql } from '@/services/api/gql';
import { updateApiToken, useAuth } from '@/services/api/auth.ts';
import { authExchange } from '@urql/exchange-auth';
import { jwtDecode } from 'jwt-decode';

const refreshTokenMutation = graphql(/* GraphQL */ `
    mutation refreshAccessToken {
        refreshAccessToken {
            accessToken
        }
    }
`);

const loadingEventsExchange: Exchange = ({ forward }) => (operations$) => {
  LOADING_STARTED_EVENT.emit();
  try {
    return forward(operations$);
  } finally {
    LOADING_FINISHED_EVENT.emit();
  }
};

const getJwtToken = () => {
    const { getToken } = useAuth();
    return getToken();
}

const jwtAuthExchange = authExchange(async utils => {
  // noinspection JSUnusedGlobalSymbols
  return {
    addAuthToOperation(operation) {
      const token = getJwtToken();
      if (!token) {
        return operation;
      }
      return utils.appendHeaders(operation, {
        Authorization: `Bearer ${token}`,
      });
    },

    didAuthError(error, _operation) {
      // see SaGrapQlException
      return error.graphQLErrors.some(e => e.extensions?.errorType === 'NOT_AUTHORIZED');
    },

    async refreshAuth() {
      const result = await utils.mutate(refreshTokenMutation, {});
      const newAccessToken = result.data?.refreshAccessToken.accessToken;
      if (newAccessToken) {
        updateApiToken(newAccessToken);
        return;
      }
    },

    willAuthError() {
      const token = getJwtToken();
      if (!token) {
        return true;
      }
      const { exp } = jwtDecode(token) as {
        exp: number,
      };
      // give some leeway of 20 seconds
      return Date.now() >= (exp * 1000 - 20000);
    },
  };
});

export const gqlClient = new Client({
  url: '/api/graphql',
  exchanges: [loadingEventsExchange, jwtAuthExchange, fetchExchange],
  fetchOptions: {
    // to enable refresh token cookie
    credentials: 'include',
    // TODO add signal: AbortSignal.timeout(15000) when https://github.com/urql-graphql/urql/issues/3801 is fixed
  },
  preferGetMethod: false,
  requestPolicy: 'network-only',
});

export function setupGqlClient() {
  provideClient(gqlClient);
}

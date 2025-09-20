import { Client, fetchExchange, provideClient } from '@urql/vue';

import type { Exchange } from '@urql/core';
import { LOADING_FINISHED_EVENT, LOADING_STARTED_EVENT } from '@/services/events';

const loadingEventsExchange: Exchange = ({ forward }) => (operations$) => {
    LOADING_STARTED_EVENT.emit();
    try {
      return forward(operations$);
    } finally {
      LOADING_FINISHED_EVENT.emit();
    }
  };

export const gqlClient = new Client({
  url: '/api/graphql',
  exchanges: [loadingEventsExchange, fetchExchange],
});

export function setupGqlClient() {
  provideClient(gqlClient);
}

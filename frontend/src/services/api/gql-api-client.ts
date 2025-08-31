import {
  Client, fetchExchange, provideClient,
} from '@urql/vue';

const client = new Client({
  url: '/api/graphql',
  exchanges: [fetchExchange],
});

export function setupGqlClient() {
  provideClient(client);
}

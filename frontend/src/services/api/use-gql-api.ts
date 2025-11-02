import { ref, Ref } from 'vue';
import { AnyVariables, DocumentInput } from '@urql/core';
import { gqlClient } from '@/services/api/gql-api-client.ts';
import useNavigation from '@/services/use-navigation.ts';
import { ApiAuthError } from '@/services/api/api-errors.ts';

export type UseGqlQueryType<Data> = [
  loading: Ref<boolean>,
  data: Ref<Data | null>,
]

export type UseGqlOptions<Variables extends AnyVariables> = {
  variables?: Variables,
}

export function useQuery<
  GqlResponse = any,
  K extends keyof GqlResponse = keyof GqlResponse,
  Variables extends AnyVariables = AnyVariables,
>(
  query: DocumentInput<GqlResponse, Variables>,
  queryName: K,
  options: UseGqlOptions<Variables> = {},
): UseGqlQueryType<GqlResponse[K]> {
  const loading: Ref<boolean> = ref(true);
  const data: Ref<GqlResponse[K] | null> = ref(null);
  const { navigateByPath } = useNavigation();

  const doLoad = async () => {
    try {
      const result = await gqlClient.query(query, options.variables);
      data.value = result[queryName];
    } catch (e: unknown) {
      if (e instanceof ApiAuthError) {
        await navigateByPath('/login');
      }
      throw e;
    } finally {
      loading.value = false;
    }
  };

  // noinspection JSIgnoredPromiseFromCall
  doLoad();

  return [loading, data];
}

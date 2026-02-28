import { nextTick, ref, Ref } from 'vue';
import { AnyVariables, DocumentInput } from '@urql/core';
import { gqlClient } from '@/services/api/gql-api-client.ts';
import useNavigation from '@/services/use-navigation.ts';
import { ApiAuthError } from '@/services/api/api-errors.ts';
import useNotifications, {
  NOTIFICATION_ALWAYS_VISIBLE_DURATION,
} from '@/components/notifications/use-notifications.ts';
import { $t } from '@/services/i18n';

type ErrorHandler = (e: unknown) => Promise<never>;

function useGqlErrorHandler(): ErrorHandler {
  const { navigateByPath } = useNavigation();
  const { showWarningNotification } = useNotifications();
  return async (e: unknown): Promise<never> => {
    if (e instanceof ApiAuthError) {
      showWarningNotification($t.value.infra.sessionExpired(), {
        duration: NOTIFICATION_ALWAYS_VISIBLE_DURATION,
      });
      await nextTick();
      await navigateByPath('/login');
    }
    throw e;
  };
}

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
  const handleError = useGqlErrorHandler();

  const doLoad = async () => {
    try {
      const result = await gqlClient.query(query, options.variables);
      data.value = result[queryName];
    } catch (e: unknown) {
      await handleError(e);
    } finally {
      loading.value = false;
    }
  };

  // noinspection JSIgnoredPromiseFromCall
  doLoad();

  return [loading, data];
}

export type MutationExecutor<GqlResponse, K extends keyof GqlResponse, Variables extends AnyVariables> = (
  variables: Variables,
) => Promise<GqlResponse[K]>;

export function useMutation<
  GqlResponse = any,
  K extends keyof GqlResponse = keyof GqlResponse,
  Variables extends AnyVariables = AnyVariables,
>(
  mutation: DocumentInput<GqlResponse, Variables>,
  mutationName: K,
): MutationExecutor<GqlResponse, K, Variables> {
  const handleError = useGqlErrorHandler();

  return async (variables: Variables): Promise<GqlResponse[K]> => {
    try {
      const result = await gqlClient.mutation(mutation, variables);
      return result[mutationName];
    } catch (e: unknown) {
      await handleError(e);
    }
  };
}

export type LazyQueryExecutor<GqlResponse, K extends keyof GqlResponse, Variables extends AnyVariables> = (
  variables: Variables,
) => Promise<GqlResponse[K]>;

export function useLazyQuery<
  GqlResponse = any,
  K extends keyof GqlResponse = keyof GqlResponse,
  Variables extends AnyVariables = AnyVariables,
>(
  query: DocumentInput<GqlResponse, Variables>,
  queryName: K,
): LazyQueryExecutor<GqlResponse, K, Variables> {
  const handleError = useGqlErrorHandler();

  return async (variables: Variables): Promise<GqlResponse[K]> => {
    try {
      const result = await gqlClient.query(query, variables);
      return result[queryName];
    } catch (e: unknown) {
      await handleError(e);
    }
  };
}

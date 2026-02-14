import type { AnyVariables, DocumentInput } from '@urql/core';
import { nextTick, type Ref, ref } from 'vue';
import useNotifications, {
  NOTIFICATION_ALWAYS_VISIBLE_DURATION,
} from '@/components/notifications/use-notifications.ts';
import { ApiAuthError } from '@/services/api/api-errors.ts';
import { gqlClient } from '@/services/api/gql-api-client.ts';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation.ts';

export type UseGqlQueryType<Data> = [loading: Ref<boolean>, data: Ref<Data | null>];

export type UseGqlOptions<Variables extends AnyVariables> = {
  variables?: Variables;
};

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
  const { showWarningNotification } = useNotifications();

  const doLoad = async () => {
    try {
      const result = await gqlClient.query(query, options.variables);
      data.value = result[queryName];
    } catch (e: unknown) {
      if (e instanceof ApiAuthError) {
        showWarningNotification($t.value.infra.sessionExpired(), {
          duration: NOTIFICATION_ALWAYS_VISIBLE_DURATION,
        });
        await nextTick();
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

export type MutationExecutor<GqlResponse, K extends keyof GqlResponse, Variables extends AnyVariables> = (
  variables: Variables,
) => Promise<GqlResponse[K]>;

export function useMutation<
  GqlResponse = any,
  K extends keyof GqlResponse = keyof GqlResponse,
  Variables extends AnyVariables = AnyVariables,
>(mutation: DocumentInput<GqlResponse, Variables>, mutationName: K): MutationExecutor<GqlResponse, K, Variables> {
  const { navigateByPath } = useNavigation();
  const { showWarningNotification } = useNotifications();

  return async (variables: Variables): Promise<GqlResponse[K]> => {
    try {
      const result = await gqlClient.mutation(mutation, variables);
      return result[mutationName];
    } catch (e: unknown) {
      if (e instanceof ApiAuthError) {
        showWarningNotification($t.value.infra.sessionExpired(), {
          duration: NOTIFICATION_ALWAYS_VISIBLE_DURATION,
        });
        await nextTick();
        await navigateByPath('/login');
      }
      throw e;
    }
  };
}

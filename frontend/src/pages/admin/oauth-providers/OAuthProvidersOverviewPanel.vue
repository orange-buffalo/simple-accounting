<template>
  <SaOverviewItem :title="provider.name">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute icon="login">
        {{ provider.clientId }}
      </SaOverviewItemPrimaryAttribute>

      <SaOverviewItemPrimaryAttribute icon="profile">
        {{ provider.userIdAttribute }}
      </SaOverviewItemPrimaryAttribute>

      <SaOverviewItemPrimaryAttribute
        v-if="provider.scopes.length > 0"
        icon="gear"
      >
        {{ provider.scopes.join(' ') }}
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #last-column>
      <SaActionLink
        icon="pencil-solid"
        @click="navigateToProviderEdit"
      >
        {{ $t.oauthProvidersOverviewPanel.edit() }}
      </SaActionLink>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
  import SaActionLink from '@/components/SaActionLink.vue';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';
  import type { OauthProvidersPageQuery } from '@/services/api/gql/graphql';

  type OAuthProviderNode = OauthProvidersPageQuery['oauthProviders']['edges'][0]['node'];

  const props = defineProps<{ provider: OAuthProviderNode }>();

  const { navigateToView } = useNavigation();
  const navigateToProviderEdit = () => navigateToView({
    name: 'edit-oauth-provider',
    params: { id: props.provider.id },
  });
</script>

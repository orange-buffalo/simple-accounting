<template>
  <SaOverviewItem :title="user.userName">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        v-if="user.admin"
        icon="admin-user"
      >
        {{ $t.adminOverviewPanel.userTypeAdmin() }}
      </SaOverviewItemPrimaryAttribute>
      <SaOverviewItemPrimaryAttribute
        v-else
        icon="regular-user"
      >
        {{ $t.adminOverviewPanel.userTypeRegular() }}
      </SaOverviewItemPrimaryAttribute>

      <SaOverviewItemPrimaryAttribute
        v-if="user.activated"
        icon="active-user"
      >
        {{ $t.adminOverviewPanel.userActivated() }}
      </SaOverviewItemPrimaryAttribute>
      <SaOverviewItemPrimaryAttribute
        v-else
        icon="inactive-user"
      >
        {{ $t.adminOverviewPanel.userNotActivated() }}
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #last-column>
      <SaActionLink
        icon="pencil-solid"
        @click="navigateToUserEdit"
      >
        {{ $t.adminOverviewPanel.edit() }}
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
  import type { UsersPageQuery } from '@/services/api/gql/graphql';

  type UserNode = UsersPageQuery['users']['edges'][0]['node'];

  const props = defineProps<{ user: UserNode }>();

  const { navigateToView } = useNavigation();
  const navigateToUserEdit = () => navigateToView({
    name: 'edit-user',
    params: { id: props.user.id },
  });
</script>

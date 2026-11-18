<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.usersOverview.header()"
    :filters="userFilters"
    :create-action-label="$t.usersOverview.create()"
    :create-action-view-name="CREATE_NEW_USER_ROUTE_NAME"
  >
    <SaPageableItems
      :page-query="usersPageQuery"
      path="users"
      :page-query-arguments="{ freeSearchText: overviewFilters.freeSearchText }"
      #default="{ item }"
    >
      <UsersOverviewPanel :user="item" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts">
  export const USERS_OVERVIEW_ROUTE_NAME = 'users-overview';
</script>

<script lang="ts" setup>
  import { CREATE_NEW_USER_ROUTE_NAME } from '@/pages/admin/users/EditUser.vue';
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import { $t } from '@/services/i18n';
  import UsersOverviewPanel from '@/pages/admin/users/UsersOverviewPanel.vue';
  import { graphql } from '@/services/api/gql';
  import {
    createOverviewFilters,
    type SaOverviewFilterConfigs,
  } from '@/components/overview-page/overview-page-filters';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';

  type UsersOverviewFilters = {
    freeSearchText: string | null,
  };

  const usersPageQuery = graphql(`
    query usersPage($first: Int!, $after: String, $freeSearchText: String) {
      users(first: $first, after: $after, freeSearchText: $freeSearchText) {
        edges {
          cursor
          node {
            id
            userName
            admin
            activated
          }
        }
        pageInfo {
          ...PaginationPageInfo
        }
        totalCount
      }
    }
  `);

  const overviewFilters = ref(createOverviewFilters<UsersOverviewFilters>({
    freeSearchText: null,
  }));
  const userFilters: SaOverviewFilterConfigs<UsersOverviewFilters> = {
    freeSearchText: {
      type: 'text',
      label: $t.value.usersOverview.filters.freeSearchText.label(),
    },
  };

</script>

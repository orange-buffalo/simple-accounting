<template>
  <SaOverviewPage
    v-model="freeSearchText"
    :header="$t.usersOverview.header()"
    :filter-placeholder="$t.usersOverview.filters.input.placeholder()"
    :create-action-label="$t.usersOverview.create()"
    create-action-view-name="create-new-user"
  >
    <SaPageableItems
      :page-query="usersPageQuery"
      path="users"
      :page-query-arguments="{ freeSearchText: freeSearchText || null }"
      #default="{ item }"
    >
      <UsersOverviewPanel :user="item" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import { $t } from '@/services/i18n';
  import UsersOverviewPanel from '@/pages/admin/users/UsersOverviewPanel.vue';
  import { graphql } from '@/services/api/gql';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';

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

  const freeSearchText = ref<string | undefined>();

</script>

<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.oauthProvidersOverview.header()"
    :filters="providerFilters"
    :create-action-label="$t.oauthProvidersOverview.create()"
    create-action-view-name="create-new-oauth-provider"
  >
    <SaPageableItems
      :page-query="oauthProvidersPageQuery"
      path="oauthProviders"
      :page-query-arguments="{ freeSearchText: overviewFilters.freeSearchText }"
      #default="{ item }"
    >
      <OAuthProvidersOverviewPanel :provider="item" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import { $t } from '@/services/i18n';
  import OAuthProvidersOverviewPanel from '@/pages/admin/oauth-providers/OAuthProvidersOverviewPanel.vue';
  import { graphql } from '@/services/api/gql';
  import {
    createOverviewFilters,
    type SaOverviewFilterConfigs,
  } from '@/components/overview-page/overview-page-filters';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';

  type OAuthProvidersOverviewFilters = {
    freeSearchText: string | null,
  };

  const oauthProvidersPageQuery = graphql(`
    query oauthProvidersPage($first: Int!, $after: String, $freeSearchText: String) {
      oauthProviders(first: $first, after: $after, freeSearchText: $freeSearchText) {
        edges {
          cursor
          node {
            id
            name
            clientId
            userIdAttribute
            scopes
          }
        }
        pageInfo {
          ...PaginationPageInfo
        }
        totalCount
      }
    }
  `);

  const overviewFilters = ref(createOverviewFilters<OAuthProvidersOverviewFilters>({
    freeSearchText: null,
  }));
  const providerFilters: SaOverviewFilterConfigs<OAuthProvidersOverviewFilters> = {
    freeSearchText: {
      type: 'text',
      label: $t.value.oauthProvidersOverview.filters.freeSearchText.label(),
    },
  };
</script>

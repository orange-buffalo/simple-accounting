<template>
  <SaOverviewPage
    v-model="freeSearchText"
    :header="$t.generalTaxesOverview.header()"
    :filter-placeholder="$t.generalTaxesOverview.filters.input.placeholder()"
  >
    <template #actions>
      <ElButton
        round
        @click="navigateToCreateTaxView"
      >
        <SaIcon icon="plus-thin" />
        {{ $t.generalTaxesOverview.create() }}
      </ElButton>
    </template>

    <SaPageableItems
      :page-query="generalTaxesPageQuery"
      path="workspace.generalTaxes"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: freeSearchText || null }"
      #default="{ item }"
    >
      <GeneralTaxOverviewPanel :tax="item" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import GeneralTaxOverviewPanel from '@/pages/settings/general-taxes/GeneralTaxOverviewPanel.vue';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';

  const generalTaxesPageQuery = graphql(`
    query generalTaxesPage($workspaceId: String!, $first: Int!, $after: String, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        generalTaxes(first: $first, after: $after, freeSearchText: $freeSearchText) {
          edges {
            cursor
            node {
              id
              title
              description
              rateInBps
            }
          }
          pageInfo {
            ...PaginationPageInfo
          }
          totalCount
        }
      }
    }
  `);

  const { navigateByViewName } = useNavigation();
  const navigateToCreateTaxView = () => navigateByViewName('create-new-general-tax');

  const { currentWorkspaceId } = useCurrentWorkspace();

  const freeSearchText = ref<string | undefined>();
</script>

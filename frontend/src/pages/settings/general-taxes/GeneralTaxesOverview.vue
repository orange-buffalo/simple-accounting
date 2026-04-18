<template>
  <div>
    <div class="sa-page-header">
      <h1>General Taxes</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <ElButton
          round
          @click="navigateToCreateTaxView"
        >
          <SaIcon icon="plus-thin" />
          Add new
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      :page-query="generalTaxesPageQuery"
      path="workspace.generalTaxes"
      :page-query-arguments="{ workspaceId: currentWorkspaceId }"
      #default="{ item }"
    >
      <GeneralTaxOverviewPanel :tax="item" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import GeneralTaxOverviewPanel from '@/pages/settings/general-taxes/GeneralTaxOverviewPanel.vue';
  import { graphql } from '@/services/api/gql';

  const generalTaxesPageQuery = graphql(`
    query generalTaxesPage($workspaceId: Long!, $first: Int!, $after: String) {
      workspace(id: $workspaceId) {
        generalTaxes(first: $first, after: $after) {
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
</script>

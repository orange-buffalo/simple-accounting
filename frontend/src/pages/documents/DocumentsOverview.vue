<template>
  <SaOverviewPage
    :header="$t.documentsOverview.header()"
    :create-action-label="$t.documentsOverview.create()"
    create-action-view-name="create-standalone-document"
    :create-action-available="isCurrentUserRegular()"
    :create-action-disabled="!currentWorkspace.editable"
  >
    <SaPageableItems
      ref="pageItems"
      :page-query="documentsPageQuery"
      path="workspace.documents"
      :page-query-arguments="{ workspaceId: currentWorkspaceId }"
      #default="{ item }"
    >
      <DocumentsOverviewPanel
        :document="item"
        @deleted="reloadDocuments"
      />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import DocumentsOverviewPanel from '@/pages/documents/DocumentsOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';
  import { useAuth } from '@/services/api';

  const { currentWorkspaceId, currentWorkspace } = useCurrentWorkspace();
  const { isCurrentUserRegular } = useAuth();
  const pageItems = ref<{ reload: () => void }>();

  const reloadDocuments = () => pageItems.value?.reload();

  const documentsPageQuery = graphql(`
    query documentsPage($workspaceId: String!, $first: Int!, $after: String) {
      workspace(id: $workspaceId) {
        documents(first: $first, after: $after) {
          edges {
            cursor
            node {
              id
              name
              timeUploaded
              storageId
              usedBy {
                type
                relatedEntityId
                displayName
              }
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
</script>

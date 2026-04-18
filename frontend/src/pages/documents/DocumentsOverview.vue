<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.documentsOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.documentsOverview.filters.announcement() }}</span>
        </div>

        <span />
      </div>
    </div>

    <SaPageableItems
      :page-query="documentsPageQuery"
      path="workspace.documents"
      :page-query-arguments="{ workspaceId: currentWorkspaceId }"
      #default="{ item }"
    >
      <DocumentsOverviewPanel :document="item" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import DocumentsOverviewPanel from '@/pages/documents/DocumentsOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';

  const { currentWorkspaceId } = useCurrentWorkspace();

  const documentsPageQuery = graphql(`
    query documentsPage($workspaceId: Long!, $first: Int!, $after: String) {
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

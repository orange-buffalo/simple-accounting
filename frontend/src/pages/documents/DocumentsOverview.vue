<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.documentsOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.documentsOverview.filters.announcement() }}</span>
        </div>

        <ElButton
          v-if="isCurrentUserRegular()"
          round
          :disabled="!currentWorkspace.editable"
          @click="navigateToCreateStandaloneDocumentView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.documentsOverview.create() }}
        </ElButton>
      </div>
    </div>

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
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaIcon from '@/components/SaIcon.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import DocumentsOverviewPanel from '@/pages/documents/DocumentsOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';
  import { useAuth } from '@/services/api';

  const { currentWorkspaceId, currentWorkspace } = useCurrentWorkspace();
  const { isCurrentUserRegular } = useAuth();
  const pageItems = ref<{ reload: () => void }>();
  const { navigateByViewName } = useNavigation();

  const reloadDocuments = () => pageItems.value?.reload();
  const navigateToCreateStandaloneDocumentView = () => navigateByViewName('create-standalone-document');

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

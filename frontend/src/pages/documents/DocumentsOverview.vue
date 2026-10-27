<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.documentsOverview.header()"
    :filters="documentFilters"
    :create-action-label="$t.documentsOverview.create()"
    create-action-view-name="create-standalone-document"
    :create-action-available="isCurrentUserRegular()"
    :create-action-disabled="!currentWorkspace.editable"
  >
    <SaPageableItems
      ref="pageItems"
      :page-query="documentsPageQuery"
      path="workspace.documents"
      :page-query-arguments="documentPageArguments"
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
  import { computed, ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import DocumentsOverviewPanel from '@/pages/documents/DocumentsOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';
  import { useAuth } from '@/services/api';
  import { DocumentUsageFilterType } from '@/services/api/gql/schema-types';
  import {
    createOverviewFilters,
    type SaOverviewFilterConfigs,
  } from '@/components/overview-page/overview-page-filters';

  type DocumentsOverviewFilters = {
    freeSearchText: string | null,
    storageIdsIn: string[] | null,
    usageTypeIn: DocumentUsageFilterType[] | null,
  };

  const { currentWorkspaceId, currentWorkspace } = useCurrentWorkspace();
  const { isCurrentUserRegular } = useAuth();
  const pageItems = ref<{ reload: () => void }>();

  const reloadDocuments = () => pageItems.value?.reload();

  const overviewFilters = ref(createOverviewFilters<DocumentsOverviewFilters>({
    freeSearchText: null,
    storageIdsIn: null,
    usageTypeIn: null,
  }));
  const documentPageArguments = computed(() => ({
    workspaceId: currentWorkspaceId,
    freeSearchText: overviewFilters.value.freeSearchText,
    storageIdsIn: overviewFilters.value.storageIdsIn,
    usageTypeIn: overviewFilters.value.usageTypeIn,
  }));

  const documentFilters = {
    freeSearchText: {
      type: 'text',
      label: $t.value.documentsOverview.filters.freeSearchText.label(),
    },
    usageTypeIn: {
      type: 'multi-select',
      label: $t.value.documentsOverview.filters.usage.label(),
      options: [
        {
          label: $t.value.documentsOverview.filters.usage.expense(),
          value: DocumentUsageFilterType.Expense,
        },
        {
          label: $t.value.documentsOverview.filters.usage.income(),
          value: DocumentUsageFilterType.Income,
        },
        {
          label: $t.value.documentsOverview.filters.usage.invoice(),
          value: DocumentUsageFilterType.Invoice,
        },
        {
          label: $t.value.documentsOverview.filters.usage.incomeTaxPayment(),
          value: DocumentUsageFilterType.IncomeTaxPayment,
        },
        {
          label: $t.value.documentsOverview.filters.usage.standaloneDocument(),
          value: DocumentUsageFilterType.StandaloneDocument,
        },
        {
          label: $t.value.documentsOverview.filters.usage.unused(),
          value: DocumentUsageFilterType.Unused,
        },
      ],
    },
    storageIdsIn: {
      type: 'multi-select',
      label: $t.value.documentsOverview.filters.storage.label(),
      options: [
        {
          label: $t.value.documentsOverviewPanel.storage.googleDrive(),
          value: 'google-drive',
        },
        {
          label: $t.value.documentsOverviewPanel.storage.internalSystem(),
          value: 'local-fs',
        },
        {
          label: $t.value.documentsOverviewPanel.storage.unknown(),
          value: 'noop',
        },
      ],
    },
  } satisfies SaOverviewFilterConfigs<DocumentsOverviewFilters>;

  const documentsPageQuery = graphql(`
    query documentsPage(
      $workspaceId: String!,
      $first: Int!,
      $after: String,
      $freeSearchText: String,
      $storageIdsIn: [String!],
      $usageTypeIn: [DocumentUsageFilterType!]
    ) {
      workspace(id: $workspaceId) {
        documents(
          first: $first,
          after: $after,
          freeSearchText: $freeSearchText,
          storageIdsIn: $storageIdsIn,
          usageTypeIn: $usageTypeIn
        ) {
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

<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.invoicesOverview.header()"
    :filters="invoiceFilters"
    :create-action-label="$t.invoicesOverview.create()"
    create-action-view-name="create-new-invoice"
    :create-action-disabled="!currentWorkspace.editable"
  >
    <SaPageableItems
      ref="pageableItemsRef"
      #default="{ item: invoice }"
      :page-query="invoicesPageQuery"
      path="workspace.invoices"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: overviewFilters.freeSearchText }"
    >
      <InvoicesOverviewPanel :invoice="invoice" @invoice-update="onInvoiceUpdate" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import InvoicesOverviewPanel from '@/pages/invoices/InvoicesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import {
    createOverviewFilters,
    type SaOverviewFilterConfigs,
  } from '@/components/overview-page/overview-page-filters';

  type InvoicesOverviewFilters = {
    freeSearchText: string | null,
  };

  const invoicesPageQuery = graphql(`
    query invoicesPage($workspaceId: String!, $first: Int!, $after: String, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        invoices(first: $first, after: $after, freeSearchText: $freeSearchText) {
          edges {
            cursor
            node {
              id
              version
              title
              dateIssued
              dateSent
              datePaid
              dueDate
              currency
              amount
              notes
              status
              customer {
                id
                name
              }
              generalTax {
                id
                title
                rateInBps
              }
              attachments {
                ...DocumentData
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

  const overviewFilters = ref(createOverviewFilters<InvoicesOverviewFilters>({
    freeSearchText: null,
  }));
  const invoiceFilters: SaOverviewFilterConfigs<InvoicesOverviewFilters> = {
    freeSearchText: {
      type: 'text',
      label: $t.value.invoicesOverview.filters.freeSearchText.label(),
    },
  };
  const pageableItemsRef = ref<{ reload: () => void } | null>(null);
  const {
    currentWorkspaceId,
    currentWorkspace,
  } = useCurrentWorkspace();

  const onInvoiceUpdate = () => {
    pageableItemsRef.value?.reload();
  };
</script>

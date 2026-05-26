<template>
  <SaOverviewPage
    v-model="invoicesFilter"
    :header="$t.invoicesOverview.header()"
    :filter-placeholder="$t.invoicesOverview.filters.input.placeholder()"
  >
    <template #actions>
      <ElButton
        round
        :disabled="!currentWorkspace.editable"
        @click="navigateToCreateInvoiceView"
      >
        <SaIcon icon="plus-thin" />
        {{ $t.invoicesOverview.create() }}
      </ElButton>
    </template>

    <SaPageableItems
      ref="pageableItemsRef"
      #default="{ item: invoice }"
      :page-query="invoicesPageQuery"
      path="workspace.invoices"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: invoicesFilter || null }"
    >
      <InvoicesOverviewPanel :invoice="invoice" @invoice-update="onInvoiceUpdate" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import InvoicesOverviewPanel from '@/pages/invoices/InvoicesOverviewPanel.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';

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

  const invoicesFilter = ref<string | undefined>(undefined);
  const pageableItemsRef = ref<{ reload: () => void } | null>(null);
  const {
    currentWorkspaceId,
    currentWorkspace,
  } = useCurrentWorkspace();

  const { navigateByViewName } = useNavigation();

  const navigateToCreateInvoiceView = () => {
    navigateByViewName('create-new-invoice');
  };

  const onInvoiceUpdate = () => {
    pageableItemsRef.value?.reload();
  };
</script>

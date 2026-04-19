<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.invoicesOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.invoicesOverview.filters.announcement() }}</span>
        </div>

        <div>
          <ElInput
            class="sa-header-options__filter-input"
            v-model="invoicesFilter"
            :placeholder="$t.invoicesOverview.filters.input.placeholder()"
            clearable
          >
            <template #prefix>
              <Search class="sa-header-options__filter-input__icon" />
            </template>
          </ElInput>
        </div>

        <ElButton
          round
          :disabled="!currentWorkspace.editable"
          @click="navigateToCreateInvoiceView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.invoicesOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      ref="pageableItemsRef"
      #default="{ item: invoice }"
      :page-query="invoicesPageQuery"
      path="workspace.invoices"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: invoicesFilter || null }"
    >
      <InvoicesOverviewPanel :invoice="invoice" @invoice-update="onInvoiceUpdate" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Search } from '@element-plus/icons-vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import InvoicesOverviewPanel from '@/pages/invoices/InvoicesOverviewPanel.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';

  const invoicesPageQuery = graphql(`
    query invoicesPage($workspaceId: Long!, $first: Int!, $after: String, $freeSearchText: String) {
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

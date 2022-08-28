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

    <PageableItems
      #default="{ item: invoice }"
      :reload-on="[invoicesFilter, invoiceUpdateTrigger]"
      :page-provider="invoicesProvider"
    >
      <InvoicesOverviewPanel
        :invoice="invoice as InvoiceDto"
        @invoice-update="onInvoiceUpdate"
      />
    </PageableItems>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Search } from '@element-plus/icons-vue';
  import PageableItems from '@/components/pageable-items/PageableItems.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import InvoicesOverviewPanel from '@/pages/invoices/InvoicesOverviewPanel.vue';
  import type { ApiPageRequest, InvoiceDto } from '@/services/api';
  import { invoicesApi } from '@/services/api';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';

  const invoicesFilter = ref<string | undefined>(undefined);
  const {
    currentWorkspaceId,
    currentWorkspace,
  } = useCurrentWorkspace();

  const invoicesProvider = async (request: ApiPageRequest, config: RequestInit) => invoicesApi.getInvoices({
    ...request,
    freeSearchTextEq: invoicesFilter.value,
    workspaceId: currentWorkspaceId,
  }, config);

  const { navigateByViewName } = useNavigation();

  const navigateToCreateInvoiceView = () => {
    navigateByViewName('create-new-invoice');
  };

  const invoiceUpdateTrigger = ref(true);
  const onInvoiceUpdate = () => {
    invoiceUpdateTrigger.value = !invoiceUpdateTrigger.value;
  };
</script>

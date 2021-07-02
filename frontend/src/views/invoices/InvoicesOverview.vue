<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t('invoicesOverview.header') }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t('invoicesOverview.filters.announcement') }}</span>
        </div>

        <div>
          <ElInput
            v-model="invoicesFilter"
            :placeholder="$t('invoicesOverview.filters.input.placeholder')"
            clearable
          >
            <i
              slot="prefix"
              class="el-icon-search el-input__icon"
            />
          </ElInput>
        </div>

        <ElButton
          round
          :disabled="!currentWorkspace.editable"
          @click="navigateToCreateInvoiceView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t('invoicesOverview.create') }}
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      v-slot="{item: invoice}"
      :items="invoices"
    >
      <InvoicesOverviewPanel
        :invoice="invoice"
        @invoice-update="onInvoiceUpdate"
      />
    </SaPageableItems>
  </div>
</template>

<script lang="ts">
  import { defineComponent, ref } from '@vue/composition-api';
  import SaPageableItems from '@/components/data/SaPageableItems';
  import SaIcon from '@/components/SaIcon';
  import { usePageableItems } from '@/components/data/pageableItems';
  import InvoicesOverviewPanel from '@/views/invoices/InvoicesOverviewPanel';
  import { apiClient, GetInvoicesParameters, InvoiceDto } from '@/services/api';
  import useNavigation from '@/components/navigation/useNavigation';
  import { useCurrentWorkspace } from '@/services/workspaces';

  export default defineComponent({
    components: {
      InvoicesOverviewPanel,
      SaIcon,
      SaPageableItems,
    },

    setup() {
      const invoicesFilter = ref<String | null>(null);
      const { currentWorkspaceId, currentWorkspace } = useCurrentWorkspace();
      const { items, reload } = usePageableItems<GetInvoicesParameters, InvoiceDto>({
        workspaceId: currentWorkspaceId,
        'freeSearchText[eq]': invoicesFilter as any,
      }, (request, config) => apiClient.getInvoices(request, null, config));
      const { navigateByViewName } = useNavigation();

      const navigateToCreateInvoiceView = () => {
        navigateByViewName('create-new-invoice');
      };

      const onInvoiceUpdate = () => {
        reload();
      };

      return {
        invoices: items,
        invoicesFilter,
        navigateToCreateInvoiceView,
        onInvoiceUpdate,
        currentWorkspace,
      };
    },
  });
</script>

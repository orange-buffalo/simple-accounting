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
            v-model="userFilters.freeSearchText"
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

    <DataItems
      #default="{item: invoice}"
      ref="invoicesList"
      :api-path="`/workspaces/${currentWorkspace.id}/invoices`"
      :filters="invoicesApiFilters"
    >
      <InvoicesOverviewPanel
        :invoice="invoice"
        @invoice-update="onInvoiceUpdate"
      />
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaIcon from '@/components/SaIcon';
  import InvoicesOverviewPanel from '@/views/invoices/InvoicesOverviewPanel';

  export default {
    name: 'InvoicesOverview',

    components: {
      InvoicesOverviewPanel,
      SaIcon,
      DataItems,
    },

    mixins: [withWorkspaces],

    data() {
      return {
        userFilters: {
          freeSearchText: null,
        },
      };
    },

    computed: {
      invoicesApiFilters() {
        // read the property to enable reactivity
        const { freeSearchText } = this.userFilters;
        return {
          applyToRequest: (pageRequest) => {
            pageRequest.eqFilter('freeSearchText', freeSearchText);
          },
        };
      },
    },

    methods: {
      navigateToCreateInvoiceView() {
        this.$router.push({ name: 'create-new-invoice' });
      },

      onInvoiceUpdate() {
        this.$refs.invoicesList.reloadData();
      },
    },
  };
</script>

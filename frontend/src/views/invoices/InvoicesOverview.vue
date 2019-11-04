<template>
  <div>
    <div class="sa-page-header">
      <h1>Invoices</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <div>
          <ElInput
            v-model="userFilters.freeSearchText"
            placeholder="Search invoices"
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
          Add new
        </ElButton>
      </div>
    </div>

    <DataItems
      ref="invoicesList"
      :api-path="`/workspaces/${currentWorkspace.id}/invoices`"
      :filters="invoicesApiFilters"
      #default="{item: invoice}"
    >
      <InvoiceOverviewPanel
        :invoice="invoice"
        @invoice-update="onInvoiceUpdate"
      />
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems';
  import InvoiceOverviewPanel from './InvoiceOverviewPanel';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaIcon from '@/components/SaIcon';

  export default {
    name: 'IncomesOverview',

    components: {
      SaIcon,
      DataItems,
      InvoiceOverviewPanel,
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

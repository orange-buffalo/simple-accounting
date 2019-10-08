<template>
  <div>
    <div class="sa-page-header">
      <h1>Invoices</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <div>
          <ElInput placeholder="Search invoices"
                   v-model="userFilters.freeSearchText"
                   clearable>
            <i class="el-icon-search el-input__icon"
               slot="prefix"></i>
          </ElInput>
        </div>

        <ElButton round
                  @click="navigateToCreateInvoiceView"
                  :disabled="!currentWorkspace.editable">
          <SaIcon icon="plus-thin"/>
          Add new
        </ElButton>
      </div>
    </div>

    <DataItems :api-path="`/workspaces/${currentWorkspace.id}/invoices`"
               ref="invoicesList"
               :filters="invoicesApiFilters"
               #default="{item: invoice}">
      <InvoiceOverviewPanel :invoice="invoice"
                            @invoice-update="onInvoiceUpdate"/>
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import InvoiceOverviewPanel from './InvoiceOverviewPanel'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import SaIcon from '@/components/SaIcon'

  export default {
    name: 'IncomesOverview',

    mixins: [withWorkspaces],

    components: {
      SaIcon,
      DataItems,
      InvoiceOverviewPanel
    },

    data: function () {
      return {
        userFilters: {
          freeSearchText: null
        }
      }
    },

    computed: {
      invoicesApiFilters: function () {
        // read the property to enable reactivity
        let freeSearchText = this.userFilters.freeSearchText
        return {
          applyToRequest: pageRequest => {
            pageRequest.eqFilter('freeSearchText', freeSearchText)
          }
        }
      }
    },

    methods: {
      navigateToCreateInvoiceView: function () {
        this.$router.push({name: 'create-new-invoice'})
      },

      onInvoiceUpdate: function () {
        this.$refs.invoicesList.reloadData()
      }
    }
  }
</script>
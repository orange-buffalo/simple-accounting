<template>
  <div>
    <div class="sa-page-header">
      <h1>Invoices</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>

        </div>

        <div>
          <el-input placeholder="Search invoices"
                    v-model="userFilters.freeSearchText"
                    clearable>
            <i class="el-icon-search el-input__icon"
               slot="prefix"></i>
          </el-input>
        </div>

        <el-button round
                   @click="navigateToCreateInvoiceView">
          <svgicon name="plus-thin"/>
          Add new
        </el-button>
      </div>
    </div>

    <h2>Pending</h2>

    <data-items :api-path="`/user/workspaces/${workspaceId}/invoices`"
                ref="pendingInvoicesList"
                :paginator="false"
                :filters="pendingInvoicesFilters">
      <template slot-scope="scope">
        <invoice-overview-panel :invoice="scope.item"
                                @invoice-update="onInvoiceUpdate"/>
      </template>
    </data-items>

    <h2>Finalized</h2>

    <data-items :api-path="`/user/workspaces/${workspaceId}/invoices`"
                ref="finalizedInvoicesList"
                :filters="finalizedInvoicesFilters">
      <template slot-scope="scope">
        <invoice-overview-panel :invoice="scope.item"
                                @invoice-update="onInvoiceUpdate"/>
      </template>
    </data-items>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import {mapState} from 'vuex'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'
  import InvoiceOverviewPanel from './InvoiceOverviewPanel'
  import {assign} from 'lodash'
  import '@/components/icons/plus-thin'

  export default {
    name: 'IncomesOverview',

    mixins: [withMediumDateFormatter],

    components: {
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
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id
      }),

      pendingInvoicesFilters: function () {
        return assign({}, this.userFilters, {
          applyToRequest: pageRequest => {
            pageRequest.eqFilter('freeSearchText', this.userFilters.freeSearchText)
            pageRequest.eqFilter('status', ['DRAFT', 'SENT', 'OVERDUE'])
          }
        })
      },

      finalizedInvoicesFilters: function () {
        return assign({}, this.userFilters, {
          applyToRequest: pageRequest => {
            pageRequest.eqFilter('freeSearchText', this.userFilters.freeSearchText)
            pageRequest.eqFilter('status', ['PAID', 'CANCELLED'])
          }
        })
      }
    },

    methods: {
      navigateToCreateInvoiceView: function () {
        this.$router.push({name: 'create-new-invoice'})
      },

      onInvoiceUpdate: function () {
        this.$refs.finalizedInvoicesList.reloadData()
        this.$refs.pendingInvoicesList.reloadData()
      }
    },

    watch: {
      finalizedInvoicesFilters: function () {
        this.$refs.finalizedInvoicesList.reloadData()
      },

      pendingInvoicesFilters: function () {
        this.$refs.pendingInvoicesList.reloadData()
      }
    }
  }
</script>
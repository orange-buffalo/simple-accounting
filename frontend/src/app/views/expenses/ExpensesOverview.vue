<template>
  <div>
    <div class="sa-page-header">
      <h1>Expenses</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <div>
          <el-input placeholder="Search expenses"
                    v-model="userFilters.freeSearchText"
                    clearable>
            <i class="el-icon-search el-input__icon"
               slot="prefix"></i>
          </el-input>
        </div>

        <el-button round
                   @click="navigateToCreateExpenseView">
          <svgicon name="plus-thin"/>
          Add new
        </el-button>
      </div>
    </div>

    <h2>Pending</h2>

    <data-items :api-path="`/user/workspaces/${currentWorkspace.id}/expenses`"
                :paginator="false"
                :filters="pendingExpensesFilters">
      <template slot-scope="scope">
        <expense-overview-panel :expense="scope.item"/>
      </template>
    </data-items>

    <h2>Finalized</h2>

    <data-items :api-path="`/user/workspaces/${currentWorkspace.id}/expenses`"
                :filters="finalizedExpensesFilters">
      <template slot-scope="scope">
        <expense-overview-panel :expense="scope.item"/>
      </template>
    </data-items>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import ExpenseOverviewPanel from './ExpenseOverviewPanel'
  import {assign} from 'lodash'
  import '@/components/icons/plus-thin'
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'

  export default {
    name: 'ExpensesOverview',

    mixins: [withWorkspaces],

    components: {
      DataItems,
      ExpenseOverviewPanel
    },

    data: function () {
      return {
        userFilters: {
          freeSearchText: null
        }
      }
    },

    computed: {
      pendingExpensesFilters: function () {
        return assign({}, this.userFilters, {
          applyToRequest: pageRequest => {
            pageRequest.eqFilter('freeSearchText', this.userFilters.freeSearchText)
            pageRequest.eqFilter('status', ['PENDING_CONVERSION', 'PENDING_ACTUAL_RATE'])
          }
        })
      },

      finalizedExpensesFilters: function () {
        return assign({}, this.userFilters, {
          applyToRequest: pageRequest => {
            pageRequest.eqFilter('freeSearchText', this.userFilters.freeSearchText)
            pageRequest.eqFilter('status', 'FINALIZED')
          }
        })
      }
    },

    methods: {
      navigateToCreateExpenseView: function () {
        this.$router.push({name: 'create-new-expense'})
      }
    }
  }
</script>
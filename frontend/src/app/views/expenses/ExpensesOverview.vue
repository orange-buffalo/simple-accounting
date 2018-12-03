<template>
  <div>
    <div class="page-header">
      <h1>Expenses</h1>

      <div class="header-options">
        <div>
          <span>Filter</span>

        </div>

        <div>
          <el-input placeholder="Search expenses"
                    v-model="userFilters.freeSearchText"
                    @change="filterExpenses"
                    @clear="clearFreeSearchText"
                    clearable>
            <i class="el-icon-search el-input__icon"
               slot="prefix"></i>
          </el-input>
        </div>

        <el-button @click="navigateToCreateExpenseView">Add new</el-button>
      </div>
    </div>

    <h2>Pending</h2>

    <data-items :api-path="`/user/workspaces/${workspaceId}/expenses`"
                ref="pendingExpensesList"
                :paginator="false"
                :filters="pendingExpensesFilters">
      <template slot-scope="scope">
        <expense-overview-panel :expense="scope.item"/>
      </template>
    </data-items>

    <h2>Finalized</h2>

    <data-items :api-path="`/user/workspaces/${workspaceId}/expenses`"
                ref="finalizedExpensesList"
                :filters="finalizedExpensesFilters">
      <template slot-scope="scope">
        <expense-overview-panel :expense="scope.item"/>
      </template>
    </data-items>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import {mapGetters, mapState} from 'vuex'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'
  import ExpenseOverviewPanel from './ExpenseOverviewPanel'
  import merge from 'merge'

  export default {
    name: 'ExpensesOverview',

    mixins: [withMediumDateFormatter],

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
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id,
        defaultCurrency: state => state.workspaces.currentWorkspace.defaultCurrency
      }),

      ...mapGetters({
        categoryById: 'workspaces/categoryById'
      }),

      pendingExpensesFilters: function () {
        return merge(true, this.userFilters, {
          applyToRequest: function (pageRequest) {
            pageRequest.eqFilter('freeSearchText', this.freeSearchText)
            pageRequest.eqFilter('status', ['PENDING_CONVERSION', 'PENDING_ACTUAL_RATE'])
          }
        })
      },

      finalizedExpensesFilters: function () {
        return merge(true, this.userFilters, {
          applyToRequest: function (pageRequest) {
            pageRequest.eqFilter('freeSearchText', this.freeSearchText)
            pageRequest.eqFilter('status', 'FINALIZED')
          }
        })
      }
    },

    methods: {
      navigateToCreateExpenseView: function () {
        this.$router.push({name: 'create-new-expense'})
      },

      filterExpenses: function () {
        this.$refs.pendingExpensesList.reloadData()
        this.$refs.finalizedExpensesList.reloadData()
      },

      clearFreeSearchText: function () {
        this.userFilters.freeSearchText = null
        this.filterExpenses()
      }
    }
  }
</script>

<style lang="scss">
  .header-options {
    display: flex;
    justify-content: space-between;
    align-items: center;

    input {
      background-color: transparent;
      border: none;
      color: grey;
      max-width: 200px;
    }
  }

  h2 {
    font-size: 140%;
  }
</style>

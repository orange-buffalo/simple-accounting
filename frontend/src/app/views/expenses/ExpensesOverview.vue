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
                    v-model="filters.freeSearchText"
                    @change="filterExpenses"
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
                :filters="filters">
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

  export default {
    name: 'ExpensesOverview',

    mixins: [withMediumDateFormatter],

    components: {
      DataItems,
      ExpenseOverviewPanel
    },

    data: function () {
      return {
        filters: {
          freeSearchText: null,
          applyToRequest: function (pageRequest) {
            pageRequest.eqFilter("freeSearchText", this.freeSearchText)
          }
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
      })
    },

    methods: {
      navigateToCreateExpenseView: function () {
        this.$router.push({name: 'create-new-expense'})
      },

      filterExpenses: function () {
        this.$refs.pendingExpensesList.reloadData()
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

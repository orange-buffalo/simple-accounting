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
  import {assign} from 'lodash'
  import '@/components/icons/plus-thin'

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
    },

    watch: {
      finalizedExpensesFilters: function () {
        this.$refs.finalizedExpensesList.reloadData()
      },

      pendingExpensesFilters: function () {
        this.$refs.pendingExpensesList.reloadData()
      }
    }
  }
</script>
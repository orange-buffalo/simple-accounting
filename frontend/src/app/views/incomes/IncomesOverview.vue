<template>
  <div>
    <div class="page-header">
      <h1>Incomes</h1>

      <div class="header-options">
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
                   @click="navigateToCreateIncomeView">
          <plus-icon/>
          Add new
        </el-button>
      </div>
    </div>

    <h2>Pending</h2>

    <data-items :api-path="`/user/workspaces/${workspaceId}/incomes`"
                ref="pendingIncomesList"
                :paginator="false"
                :filters="pendingIncomesFilters">
      <template slot-scope="scope">
        <income-overview-panel :income="scope.item"/>
      </template>
    </data-items>

    <h2>Finalized</h2>

    <data-items :api-path="`/user/workspaces/${workspaceId}/incomes`"
                ref="finalizedIncomesList"
                :filters="finalizedIncomesFilters">
      <template slot-scope="scope">
        <income-overview-panel :income="scope.item"/>
      </template>
    </data-items>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import {mapGetters, mapState} from 'vuex'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'
  import IncomeOverviewPanel from './IncomeOverviewPanel'
  import {assign} from 'lodash'
  import PlusIcon from 'vue-material-design-icons/Plus'

  export default {
    name: 'IncomesOverview',

    mixins: [withMediumDateFormatter],

    components: {
      DataItems,
      IncomeOverviewPanel,
      PlusIcon
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

      pendingIncomesFilters: function () {
        return assign({}, this.userFilters, {
          applyToRequest: pageRequest => {
            pageRequest.eqFilter('freeSearchText', this.userFilters.freeSearchText)
            pageRequest.eqFilter('status', ['PENDING_CONVERSION', 'PENDING_ACTUAL_RATE'])
          }
        })
      },

      finalizedIncomesFilters: function () {
        return assign({}, this.userFilters, {
          applyToRequest: pageRequest => {
            pageRequest.eqFilter('freeSearchText', this.userFilters.freeSearchText)
            pageRequest.eqFilter('status', 'FINALIZED')
          }
        })
      }
    },

    methods: {
      navigateToCreateIncomeView: function () {
        this.$router.push({name: 'create-new-income'})
      }
    },

    watch: {
      finalizedIncomesFilters: function () {
        this.$refs.finalizedIncomesList.reloadData()
      },

      pendingIncomesFilters: function () {
        this.$refs.pendingIncomesList.reloadData()
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

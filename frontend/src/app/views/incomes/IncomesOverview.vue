<template>
  <div>
    <div class="sa-page-header">
      <h1>Incomes</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <div>
          <el-input placeholder="Search incomes"
                    v-model="userFilters.freeSearchText"
                    clearable>
            <i class="el-icon-search el-input__icon"
               slot="prefix"></i>
          </el-input>
        </div>

        <el-button round
                   @click="navigateToCreateIncomeView">
          <svgicon name="plus-thin"/>
          Add new
        </el-button>
      </div>
    </div>

    <h2>Pending</h2>

    <data-items :api-path="`/user/workspaces/${currentWorkspace.id}/incomes`"
                :paginator="false"
                :filters="pendingIncomesFilters">
      <template slot-scope="scope">
        <income-overview-panel :income="scope.item"/>
      </template>
    </data-items>

    <h2>Finalized</h2>

    <data-items :api-path="`/user/workspaces/${currentWorkspace.id}/incomes`"
                :filters="finalizedIncomesFilters">
      <template slot-scope="scope">
        <income-overview-panel :income="scope.item"/>
      </template>
    </data-items>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import IncomeOverviewPanel from './IncomeOverviewPanel'
  import {assign} from 'lodash'
  import '@/components/icons/plus-thin'
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'

  export default {
    name: 'IncomesOverview',

    mixins: [withWorkspaces],

    components: {
      DataItems,
      IncomeOverviewPanel
    },

    data: function () {
      return {
        userFilters: {
          freeSearchText: null
        }
      }
    },

    computed: {
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
    }
  }
</script>
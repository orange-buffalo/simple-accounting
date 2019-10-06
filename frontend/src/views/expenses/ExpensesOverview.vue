<template>
  <div>
    <div class="sa-page-header">
      <h1>Expenses</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <div>
          <ElInput placeholder="Search expenses"
                   v-model="userFilters.freeSearchText"
                   clearable>
            <i class="el-icon-search el-input__icon"
               slot="prefix"></i>
          </ElInput>
        </div>

        <ElButton round
                  @click="navigateToCreateExpenseView"
                  :disabled="!currentWorkspace.editable">
          <SaIcon icon="plus-thin"/>
          Add new
        </ElButton>
      </div>
    </div>

    <DataItems :api-path="`/workspaces/${currentWorkspace.id}/expenses`"
               :filters="apiFilters"
               #default="{item: expense}">
      <ExpenseOverviewPanel :expense="expense"/>
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import ExpenseOverviewPanel from './ExpenseOverviewPanel'
  import {assign} from 'lodash'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import SaIcon from '@/components/SaIcon'

  export default {
    name: 'ExpensesOverview',

    mixins: [withWorkspaces],

    components: {
      DataItems,
      ExpenseOverviewPanel,
      SaIcon
    },

    data: function () {
      return {
        userFilters: {
          freeSearchText: null
        }
      }
    },

    computed: {
      apiFilters: function () {
        // read the value to support reactivity
        let freeSearchText = this.userFilters.freeSearchText
        return assign({}, {
          applyToRequest: pageRequest => {
            pageRequest.eqFilter('freeSearchText', freeSearchText)
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
<template>
  <div>
    <div class="sa-page-header">
      <h1>Incomes</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <div>
          <ElInput placeholder="Search incomes"
                   v-model="userFilters.freeSearchText"
                   clearable>
            <i class="el-icon-search el-input__icon"
               slot="prefix"></i>
          </ElInput>
        </div>

        <ElButton round
                  @click="navigateToCreateIncomeView"
                  :disabled="!currentWorkspace.editable">
          <SaIcon icon="plus-thin"/>
          Add new
        </ElButton>
      </div>
    </div>

    <DataItems :api-path="`/workspaces/${currentWorkspace.id}/incomes`"
               :filters="apiFilters"
               #default="{item: income}">
      <IncomeOverviewPanel :income="income"/>
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import IncomeOverviewPanel from './IncomeOverviewPanel'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import SaIcon from '@/components/SaIcon'

  export default {
    name: 'IncomesOverview',

    mixins: [withWorkspaces],

    components: {
      SaIcon,
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
      apiFilters: function () {
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
      navigateToCreateIncomeView: function () {
        this.$router.push({name: 'create-new-income'})
      }
    }
  }
</script>
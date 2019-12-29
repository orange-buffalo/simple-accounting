<template>
  <div>
    <div class="sa-page-header">
      <h1>Incomes</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <div>
          <ElInput
            v-model="userFilters.freeSearchText"
            placeholder="Search incomes"
            clearable
          >
            <i
              slot="prefix"
              class="el-icon-search el-input__icon"
            />
          </ElInput>
        </div>

        <ElButton
          round
          :disabled="!currentWorkspace.editable"
          @click="navigateToCreateIncomeView"
        >
          <SaIcon icon="plus-thin" />
          Add new
        </ElButton>
      </div>
    </div>

    <DataItems
      :api-path="`/workspaces/${currentWorkspace.id}/incomes`"
      :filters="apiFilters"
      #default="{item: income}"
    >
      <IncomesOverviewPanel :income="income" />
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaIcon from '@/components/SaIcon';
  import IncomesOverviewPanel from '@/views/incomes/IncomesOverviewPanel';

  export default {
    name: 'IncomesOverview',

    components: {
      IncomesOverviewPanel,
      SaIcon,
      DataItems,
    },

    mixins: [withWorkspaces],

    data() {
      return {
        userFilters: {
          freeSearchText: null,
        },
      };
    },

    computed: {
      apiFilters() {
        // read the property to enable reactivity
        const { freeSearchText } = this.userFilters;
        return {
          applyToRequest: (pageRequest) => {
            pageRequest.eqFilter('freeSearchText', freeSearchText);
          },
        };
      },
    },

    methods: {
      navigateToCreateIncomeView() {
        this.$router.push({ name: 'create-new-income' });
      },
    },
  };
</script>

<template>
  <div>
    <div class="sa-page-header">
      <h1>Expenses</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <div>
          <ElInput
            v-model="userFilters.freeSearchText"
            placeholder="Search expenses"
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
          @click="navigateToCreateExpenseView"
        >
          <SaIcon icon="plus-thin" />
          Add new
        </ElButton>
      </div>
    </div>

    <DataItems
      :api-path="`/workspaces/${currentWorkspace.id}/expenses`"
      :filters="apiFilters"
      #default="{item: expense}"
    >
      <ExpenseOverviewPanel :expense="expense" />
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems';
  import ExpenseOverviewPanel from './ExpenseOverviewPanel';
  import { withWorkspaces } from '@/components/mixins/with-workspaces';
  import SaIcon from '@/components/SaIcon';

  export default {
    name: 'ExpensesOverview',

    components: {
      DataItems,
      ExpenseOverviewPanel,
      SaIcon,
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
        // read the value to support reactivity
        const { freeSearchText } = this.userFilters;
        return {
          applyToRequest: (pageRequest) => {
            pageRequest.eqFilter('freeSearchText', freeSearchText);
          },
        };
      },
    },

    methods: {
      navigateToCreateExpenseView() {
        this.$router.push({ name: 'create-new-expense' });
      },
    },
  };
</script>

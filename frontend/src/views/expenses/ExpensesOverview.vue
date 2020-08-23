<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t('expensesOverview.header') }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t('expensesOverview.filters.announcement') }}</span>
        </div>

        <div>
          <ElInput
            v-model="userFilters.freeSearchText"
            :placeholder="$t('expensesOverview.filters.input.placeholder')"
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
          {{ $t('expensesOverview.create') }}
        </ElButton>
      </div>
    </div>

    <DataItems
      #default="{item: expense}"
      :api-path="`/workspaces/${currentWorkspace.id}/expenses`"
      :filters="apiFilters"
    >
      <ExpensesOverviewPanel :expense="expense" />
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaIcon from '@/components/SaIcon';
  import ExpensesOverviewPanel from '@/views/expenses/ExpensesOverviewPanel';

  export default {
    name: 'ExpensesOverview',

    components: {
      ExpensesOverviewPanel,
      DataItems,
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

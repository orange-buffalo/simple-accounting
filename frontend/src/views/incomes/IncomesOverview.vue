<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t('incomesOverview.header') }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t('incomesOverview.filters.announcement') }}</span>
        </div>

        <div>
          <ElInput
            v-model="userFilters.freeSearchText"
            :placeholder="$t('incomesOverview.filters.input.placeholder')"
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
          {{ $t('incomesOverview.create') }}
        </ElButton>
      </div>
    </div>

    <DataItems
      #default="{item: income}"
      :api-path="`/workspaces/${currentWorkspace.id}/incomes`"
      :filters="apiFilters"
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

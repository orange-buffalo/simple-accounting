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
            v-model="freeSearchText"
            :placeholder="$t('incomesOverview.filters.input.placeholder')"
            clearable
          >
            <template #prefix>
              <i class="el-icon-search el-input__icon" />
            </template>
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

<script lang="ts">
  import { computed, defineComponent, ref } from '@vue/composition-api';
  import DataItems from '@/components/DataItems';
  import SaIcon from '@/components/SaIcon';
  import IncomesOverviewPanel from '@/views/incomes/IncomesOverviewPanel';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/components/navigation/useNavigation';

  export default defineComponent({
    components: {
      IncomesOverviewPanel,
      SaIcon,
      DataItems,
    },

    setup() {
      const { currentWorkspace } = useCurrentWorkspace();

      const freeSearchText = ref<String | null>(null);

      const apiFilters = computed(() => {
        const filterValue = freeSearchText.value;
        return ({
          applyToRequest: (pageRequest: any) => {
            pageRequest.eqFilter('freeSearchText', filterValue);
          },
        });
      });

      const { navigateByViewName } = useNavigation();
      const navigateToCreateIncomeView = () => navigateByViewName('create-new-income');

      return {
        currentWorkspace,
        freeSearchText,
        apiFilters,
        navigateToCreateIncomeView,
      };
    },
  });
</script>

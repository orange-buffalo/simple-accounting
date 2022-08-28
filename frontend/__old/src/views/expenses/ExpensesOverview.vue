<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.expensesOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.expensesOverview.filters.announcement() }}</span>
        </div>

        <div>
          <ElInput
            v-model="freeSearchText"
            :placeholder="$t.expensesOverview.filters.input.placeholder()"
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
          @click="navigateToCreateExpenseView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.expensesOverview.create() }}
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

<script lang="ts">
  import { computed, defineComponent, ref } from '@vue/composition-api';
  import SaIcon from '@/components/SaIcon';
  import ExpensesOverviewPanel from '@/views/expenses/ExpensesOverviewPanel';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import DataItems from '@/components/DataItems.vue';
  import useNavigation from '@/components/navigation/useNavigation';

  export default defineComponent({
    components: {
      ExpensesOverviewPanel,
      DataItems,
      SaIcon,
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
      const navigateToCreateExpenseView = () => navigateByViewName('create-new-expense');

      return {
        currentWorkspace,
        freeSearchText,
        apiFilters,
        navigateToCreateExpenseView,
      };
    },
  });
</script>

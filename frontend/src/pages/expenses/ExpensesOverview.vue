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

    <SaPageableItems
      #default="{ item: expense }"
      :page-provider="expensesProvider"
      :reload-on="[freeSearchText]"
    >
      <ExpensesOverviewPanel :expense="expense as ExpenseDto" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
import SaIcon from '@/components/SaIcon.vue';
import ExpensesOverviewPanel from '@/pages/expenses/ExpensesOverviewPanel.vue';
import type { ApiPageRequest, ExpenseDto } from '@/services/api';
import { expensesApi } from '@/services/api';
import { $t } from '@/services/i18n/i18n-services';
import useNavigation from '@/services/use-navigation';
import { useCurrentWorkspace } from '@/services/workspaces';

const { currentWorkspaceId, currentWorkspace } = useCurrentWorkspace();

const freeSearchText = ref<string | undefined>();

const expensesProvider = async (request: ApiPageRequest, config: RequestInit) =>
  expensesApi.getExpenses(
    {
      ...request,
      freeSearchTextEq: freeSearchText.value,
      workspaceId: currentWorkspaceId,
    },
    config,
  );

const { navigateByViewName } = useNavigation();
const navigateToCreateExpenseView = () => navigateByViewName('create-new-expense');
</script>

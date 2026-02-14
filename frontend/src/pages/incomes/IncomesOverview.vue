<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.incomesOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.incomesOverview.filters.announcement() }}</span>
        </div>

        <div>
          <ElInput
            v-model="freeSearchText"
            :placeholder="$t.incomesOverview.filters.input.placeholder()"
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
          {{ $t.incomesOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      #default="{ item: income }"
      :page-provider="incomesProvider"
      :reload-on="[freeSearchText]"
    >
      <IncomesOverviewPanel :income="income as IncomeDto" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
import SaIcon from '@/components/SaIcon.vue';
import IncomesOverviewPanel from '@/pages/incomes/IncomesOverviewPanel.vue';
import type { ApiPageRequest, IncomeDto } from '@/services/api';
import { incomesApi } from '@/services/api';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { useCurrentWorkspace } from '@/services/workspaces';

const { currentWorkspaceId, currentWorkspace } = useCurrentWorkspace();

const freeSearchText = ref<string | undefined>();

const incomesProvider = async (request: ApiPageRequest, config: RequestInit) =>
  incomesApi.getIncomes(
    {
      ...request,
      workspaceId: currentWorkspaceId,
      freeSearchTextEq: freeSearchText.value,
    },
    config,
  );

const { navigateByViewName } = useNavigation();
const navigateToCreateIncomeView = () => navigateByViewName('create-new-income');
</script>

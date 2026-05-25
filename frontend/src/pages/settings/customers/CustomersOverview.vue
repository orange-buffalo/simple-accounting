<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.customersOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.customersOverview.filters.announcement() }}</span>
        </div>

        <div>
          <ElInput
            class="sa-header-options__filter-input"
            v-model="freeSearchText"
            :placeholder="$t.customersOverview.filters.input.placeholder()"
            clearable
          >
            <template #prefix>
              <Search class="sa-header-options__filter-input__icon" />
            </template>
          </ElInput>
        </div>

        <ElButton
          round
          @click="navigateToCreateCustomerView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.customersOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      :page-query="customersPageQuery"
      path="workspace.customers"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: freeSearchText || null }"
      #default="{ item }"
    >
      <CustomersOverviewPanel :customer="item" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Search } from '@element-plus/icons-vue';
  import SaIcon from '@/components/SaIcon.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import CustomersOverviewPanel from '@/pages/settings/customers/CustomersOverviewPanel.vue';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';

  const customersPageQuery = graphql(`
    query customersPage($workspaceId: String!, $first: Int!, $after: String, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        customers(first: $first, after: $after, freeSearchText: $freeSearchText) {
          edges {
            cursor
            node {
              id
              name
            }
          }
          pageInfo {
            ...PaginationPageInfo
          }
          totalCount
        }
      }
    }
  `);

  const { navigateByViewName } = useNavigation();
  const navigateToCreateCustomerView = () => navigateByViewName('create-new-customer');

  const { currentWorkspaceId } = useCurrentWorkspace();

  const freeSearchText = ref<string | undefined>();
</script>

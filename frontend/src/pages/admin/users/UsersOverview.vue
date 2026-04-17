<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.usersOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.usersOverview.filters.announcement() }}</span>
        </div>

        <div>
          <ElInput
            v-model="freeSearchText"
            :placeholder="$t.usersOverview.filters.input.placeholder()"
            clearable
          >
            <template #prefix>
              <i class="el-icon-search el-input__icon" />
            </template>
          </ElInput>
        </div>

        <ElButton
          round
          @click="navigateToCreateUserView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.usersOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItemsGql
      :page-query="usersPageQuery"
      path="users"
      :page-query-arguments="{ freeSearchText: freeSearchText || null }"
      #default="{ item }"
    >
      <UsersOverviewPanel :user="item" />
    </SaPageableItemsGql>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaIcon from '@/components/SaIcon.vue';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import UsersOverviewPanel from '@/pages/admin/users/UsersOverviewPanel.vue';
  import { graphql } from '@/services/api/gql';
  import SaPageableItemsGql from '@/components/pageable-items/SaPageableItemsGql.vue';

  const usersPageQuery = graphql(`
    query usersPage($first: Int!, $after: String, $freeSearchText: String) {
      users(first: $first, after: $after, freeSearchText: $freeSearchText) {
        edges {
          cursor
          node {
            id
            userName
            admin
            activated
          }
        }
        pageInfo {
          ...PaginationPageInfo
        }
        totalCount
      }
    }
  `);

  const freeSearchText = ref<string | undefined>();

  const { navigateByViewName } = useNavigation();
  const navigateToCreateUserView = () => navigateByViewName('create-new-user');
</script>

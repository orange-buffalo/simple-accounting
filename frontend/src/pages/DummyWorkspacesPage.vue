<template>
  <div>
    <h1>Workspaces (Dummy Page)</h1>
    <SaPageableItemsGql
      :page-query="workspacesPageQuery"
      path="workspaces"
      #default="{ item }"
    >
      <div>{{ item.name }}</div>
    </SaPageableItemsGql>
  </div>
</template>

<script lang="ts" setup>
  import { graphql } from '@/services/api/gql';
  import SaPageableItemsGql from '@/components/pageable-items/SaPageableItemsGql.vue';

  const workspacesPageQuery = graphql(`
    query workspacesPage($first: Int!, $after: String) {
      workspaces(first: $first, after: $after) {
        edges {
          cursor
          node {
            name
          }
        }
        pageInfo {
          ...PaginationPageInfo
        }
        totalCount
      }
    }
  `);
</script>

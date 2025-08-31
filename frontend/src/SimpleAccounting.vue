<template>
  <div>
    <RouterView />
  </div>
</template>

<script lang="ts" setup>
  import { setupGqlClient } from '@/services/api/gql-api-client.ts';

  // initialize the GraphQL API client (providing to child components)
  setupGqlClient();
</script>

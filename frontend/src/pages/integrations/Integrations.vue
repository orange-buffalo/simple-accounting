<template>
  <IntegrationPage :header="$t.integrations.header()" v-slot="{ workspaceId }">
    <IntegrationProviders :key="workspaceId" :workspace-id="workspaceId" />
  </IntegrationPage>
</template>

<script lang="ts" setup>
  import IntegrationPage from './IntegrationPage.vue';
  import IntegrationProviders from './IntegrationProviders.vue';
  import { $t } from '@/services/i18n';
</script>

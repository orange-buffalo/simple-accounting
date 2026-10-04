<template>
  <IntegrationPage :header="$t.integrations.wise.setupHeader()" v-slot="{ workspaceId }">
    <WiseSetupWizard :key="workspaceId" :workspace-id="workspaceId" />
  </IntegrationPage>
</template>

<script lang="ts" setup>
  import IntegrationPage from './IntegrationPage.vue';
  import WiseSetupWizard from './WiseSetupWizard.vue';
  import { $t } from '@/services/i18n';
</script>

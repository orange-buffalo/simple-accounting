<template>
  <SaPage :header="$t.integrations.header()">
    <IntegrationProviders :workspace-id="workspaceId" />
  </SaPage>
</template>

<script lang="ts" setup>
  import SaPage from '@/components/SaPage.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import IntegrationProviders from './IntegrationProviders.vue';
  import { $t } from '@/services/i18n';
  const workspaceId = useCurrentWorkspace().currentWorkspace.id;
</script>

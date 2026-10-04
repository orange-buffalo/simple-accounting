<template>
  <IntegrationPage :header="$t.integrations.wise.viewHeader()" />
</template>

<script lang="ts" setup>
  import IntegrationPage from './IntegrationPage.vue';
  import { $t } from '@/services/i18n';
</script>

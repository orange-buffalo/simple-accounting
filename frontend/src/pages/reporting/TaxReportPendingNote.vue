<template>
  <ElAlert
    type="warning"
    :title="$t.reporting.pending.excludedFromTotals()"
    :closable="false"
    center
    class="tax-report-pending-note"
  />
</template>

<script lang="ts" setup>
  import { $t } from '@/services/i18n';
</script>

<style lang="scss" scoped>
  .tax-report-pending-note {
    margin-top: 1em;
    text-align: center;
  }
</style>

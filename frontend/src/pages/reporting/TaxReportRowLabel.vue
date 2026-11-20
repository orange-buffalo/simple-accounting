<template>
  <div class="tax-report-row-label">
    <span v-if="name">{{ name }}</span>
    <SaStatusLabel v-if="!finalized" status="pending" hide-icon>
      {{ $t.reporting.pending.label() }}
    </SaStatusLabel>
  </div>
</template>

<script lang="ts" setup>
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { $t } from '@/services/i18n';

  defineProps<{
    name?: string;
    finalized: boolean;
  }>();
</script>

<style lang="scss" scoped>
  .tax-report-row-label {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px 8px;
  }
</style>

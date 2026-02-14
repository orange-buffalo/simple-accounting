<template>
  <div class="overview-item-amount-panel">
    <SaMoneyOutput
      :amount-in-cents="amount"
      :currency="currency"
    />
  </div>
</template>

<script lang="ts" setup>
import SaMoneyOutput from '@/components/SaMoneyOutput.vue';

defineProps<{
  currency: string;
  amount: number;
}>();
</script>

<style lang="scss">
  .overview-item-amount-panel {
    height: 100%;
    display: flex;
    width: 100%;
    align-items: center;
    justify-content: flex-end;
    font-size: 115%;
  }
</style>

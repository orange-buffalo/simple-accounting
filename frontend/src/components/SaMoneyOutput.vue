<template>
  <span>{{
    (amountInCents === null || amountInCents === undefined)
      ? ''
      : $t.common.amount.withCurrency(amountInCents, currency)
  }}</span>
</template>

<script lang="ts" setup>
import { $t } from '@/services/i18n';

defineProps({
  currency: {
    type: String,
    required: true,
  },
  amountInCents: {
    type: Number,
    default: null,
  },
});
</script>

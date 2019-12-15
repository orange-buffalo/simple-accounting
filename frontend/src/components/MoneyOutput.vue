<template>
  <span>{{ $t('common.amount.withCurrency', [{ amount, currency }]) }}</span>
</template>

<script>
  export default {
    name: 'MoneyOutput',

    props: {
      currency: {
        type: String,
        required: true,
      },
      amount: {
        type: Number,
        required: true,
      },
    },
  };
</script>

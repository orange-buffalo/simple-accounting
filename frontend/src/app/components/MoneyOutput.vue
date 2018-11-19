<template>
  <span>{{ amountLabel }}</span>
</template>

<script>
  import withCurrencyFormatter from '@/app/components/mixins/with-currency-formatter'

  export default {
    name: 'MoneyOutput',

    mixins: [withCurrencyFormatter],

    props: {
      currency: String,
      amount: Number
    },

    computed: {

      //todo retrieve currency precision and convert from cents to currency units

      amountLabel: function () {
        return this.currencyFormatter(this.amount)
      }
    }
  }
</script>
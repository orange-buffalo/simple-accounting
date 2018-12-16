<template>
  <span>{{ amountLabel }}</span>
</template>

<script>
  import withCurrencyFormatter from '@/app/components/mixins/with-currency-formatter'
  import {withCurrencyInfo} from '@/app/components/mixins/with-currency-info'

  export default {
    name: 'MoneyOutput',

    mixins: [withCurrencyFormatter, withCurrencyInfo],

    props: {
      currency: String,
      amount: Number
    },

    computed: {
      amountLabel: function () {
        return this.currencyFormatter(this.amount / Math.pow(10, this.currencyDigits(this.currency)))
      }
    }
  }
</script>
<template>
  <span>{{ amountLabel }}</span>
</template>

<script>
  import {mapGetters} from 'vuex'

  export default {
    name: 'MoneyOutput',

    props: {
      currency: String,
      amount: Number
    },

    computed: {
      ...mapGetters({
        getCurrencyFormatter: 'i18n/getCurrencyFormatter'
      }),

      //todo retrieve currency precision and convert from cents to currency units

      amountLabel: function () {
        return this.getCurrencyFormatter(this.currency)(this.amount)
      }
    }
  }
</script>
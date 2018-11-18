<template>
  <div class="money-output">
    <span>{{ defaultCurrencyAmountLabel }}</span>
    <template v-if="amountConverted">
      <br/>
      <span class="original-amount">{{ originalCurrencyAmountLabel}}</span>
    </template>
  </div>
</template>

<script>
  import {mapState, mapGetters, mapActions} from 'vuex'

  export default {
    name: 'MoneyOutput',

    props: {
      currency: String,
      originalAmount: Number,
      amountInDefaultCurrency: Number,
      actualAmountInDefaultCurrency: Number
    },

    data: function () {
      return {}
    },

    created: function () {

    },

    methods: {
      ...mapActions('app', {
        loadCurrencies: 'loadCurrencies'
      })


    },

    computed: {
      ...mapState({
        defaultCurrency: state => state.workspaces.currentWorkspace.defaultCurrency
      }),

      ...mapGetters({
        getCurrencyFormatter: 'i18n/getCurrencyFormatter'
      }),

      //todo retrieve currency precision and convert from cents to currency units

      defaultCurrencyAmountLabel: function () {
        if (this.amountConverted) {
          return this.getCurrencyFormatter(this.defaultCurrency)(this.amountInDefaultCurrency)
        }
        else {
          return this.getCurrencyFormatter(this.defaultCurrency)(this.originalAmount)
        }
      },

      originalCurrencyAmountLabel: function () {
        return this.getCurrencyFormatter(this.currency)(this.originalAmount)
      },

      amountConverted: function () {
        return this.currency !== this.defaultCurrency
      }


    }
  }
</script>

<style lang="scss">
  .money-output {
    text-align: right;

    .original-amount {
      font-size: 90%;
      color: #606060;
    }

  }
</style>
<template>
  <el-select v-model="inputValue"
             filterable
             :placeholder="placeholder">
    <el-option
        v-for="currency in currenciesData"
        :key="currency.code"
        :label="currencyLabel(currency)"
        :value="currency.code">
      <div class="currency-input">
        <span class="currency-code">{{currency.code}}</span>
        <span class="currency-name">{{currency.displayName}}</span>
      </div>
    </el-option>
  </el-select>
</template>

<script>
  import {mapState, mapActions} from 'vuex'
  import {withCurrencyInfo} from '@/app/components/mixins/with-currency-info'

  export default {
    name: 'CurrencyInput',

    mixins: [withCurrencyInfo],

    props: {
      value: String,
      placeholder: String
    },

    data: function () {
      return {
        inputValue: this.value
      }
    },

    created: function () {
      this.loadCurrencies()
      if (!this.inputValue) {
        this.inputValue = this.defaultCurrency
      }
    },

    methods: {
      ...mapActions('app', {
        loadCurrencies: 'loadCurrencies'
      }),

      currencyLabel: function (currency) {
        return `${currency.code} - ${currency.displayName}`
      }
    },

    computed: {
      ...mapState({
        currencies: state => state.app.currencies,
        defaultCurrency: state => state.workspaces.currentWorkspace.defaultCurrency
      }),

      currenciesData: function () {
        return this.currencies.map(currency => {
          return {
            code: currency.code,
            symbol: this.currencySymbol(currency.code),
            displayName: this.currencyDisplayName(currency.code)
          }
        })
      }
    },

    watch: {
      value: function (val) {
        this.inputValue = val
      },

      inputValue: function(val) {
        this.$emit('input', val);
      }
    }
  }
</script>

<style lang="scss">
  .currency-input {

    .currency-code {
      font-size: 110%;
      display: inline-block;
      min-width: 3em;
    }

    .currency-name {
      display: inline-block;
      margin-left: 5px;
      color: #6e716f;
      font-size: 90%;
    }
  }
</style>
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
  import api from '@/services/api'
  import {mapState, mapGetters, mapActions} from 'vuex'

  export default {
    name: 'CurrencyInput',

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

      ...mapGetters({
        getCurrencyInfo: 'i18n/getCurrencyInfo'
      }),

      currenciesData: function () {
        return this.currencies.map(currency => {
          let currencyInfo = this.getCurrencyInfo(currency.code)
          return {
            code: currency.code,
            symbol: currencyInfo.symbol,
            displayName: currencyInfo.displayName
          }
        })
      }
    },

    watch: {
      inputValue(val) {
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
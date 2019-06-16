<template>
  <el-select v-model="inputValue"
             filterable
             :placeholder="placeholder"
             :disabled="disabled">
    <el-option-group
        v-for="group in currenciesGroups"
        :key="group.title"
        :label="group.title">
      <el-option
          v-for="currency in currenciesData(group.currencies)"
          :key="currency.code"
          :label="currencyLabel(currency)"
          :value="currency.code">
        <div class="currency-input">
          <span class="currency-code">{{currency.code}}</span>
          <span class="currency-name">{{currency.displayName}}</span>
        </div>
      </el-option>
    </el-option-group>
  </el-select>
</template>

<script>
  import {mapState, mapActions} from 'vuex'
  import {withCurrencyInfo} from '@/components/mixins/with-currency-info'
  import {api} from '@/services/api'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'

  export default {
    name: 'CurrencyInput',

    mixins: [withCurrencyInfo, withWorkspaces],

    props: {
      value: String,
      placeholder: String,
      disabled: Boolean
    },

    data: function () {
      return {
        inputValue: this.value,
        currenciesShortlist: []
      }
    },

    created: async function () {
      this.loadCurrencies()
      if (!this.inputValue) {
        this.inputValue = this.defaultCurrency
      }

      let currenciesShortlistResponse = await api
          .get(`/workspaces/${this.currentWorkspace.id}/statistics/currencies-shortlist`)
      this.currenciesShortlist = currenciesShortlistResponse.data
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
        return currencies => currencies.map(currency => {
          return {
            code: currency.code,
            symbol: this.currencySymbol(currency.code),
            displayName: this.currencyDisplayName(currency.code)
          }
        })
      },

      currenciesGroups: function () {
        let groups = []
        if (this.currenciesShortlist.length > 0) {
          // todo #6: i18n
          groups.push({
            title: 'Recently Used Currencies',
            currencies: this.currenciesShortlist.map(it => {
              return {code: it}
            })
          })
        }
        groups.push({
          title: 'All Currencies',
          currencies: this.currencies
        })
        return groups
      }
    },

    watch: {
      value: function (val) {
        this.inputValue = val
      },

      inputValue: function (val) {
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
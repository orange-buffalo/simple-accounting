<template>
  <ElSelect
    v-model="inputValue"
    filterable
    :placeholder="placeholder"
    :disabled="disabled"
  >
    <ElOptionGroup
      v-for="group in currenciesGroups"
      :key="group.title"
      :label="group.title"
    >
      <ElOption
        v-for="currency in currenciesData(group.currencies)"
        :key="currency.code"
        :label="currencyLabel(currency)"
        :value="currency.code"
      >
        <div class="currency-input">
          <span class="currency-code">{{ currency.code }}</span>
          <span class="currency-name">{{ currency.displayName }}</span>
        </div>
      </ElOption>
    </ElOptionGroup>
  </ElSelect>
</template>

<script>
  import { mapState, mapActions } from 'vuex';
  import { api } from '@/services/api';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import i18n from '@/services/i18n';

  export default {
    name: 'CurrencyInput',

    mixins: [withWorkspaces],

    props: {
      value: {
        type: String,
        default: null,
      },
      placeholder: {
        type: String,
        default: null,
      },
      disabled: {
        type: Boolean,
        default: false,
      },
    },

    data() {
      return {
        inputValue: this.value,
        currenciesShortlist: [],
      };
    },

    async created() {
      this.loadCurrencies();
      if (!this.inputValue) {
        this.inputValue = this.defaultCurrency;
      }

      const currenciesShortlistResponse = await api
        .get(`/workspaces/${this.currentWorkspace.id}/statistics/currencies-shortlist`);
      this.currenciesShortlist = currenciesShortlistResponse.data;
    },

    methods: {
      ...mapActions('app', {
        loadCurrencies: 'loadCurrencies',
      }),

      currencyLabel(currency) {
        return `${currency.code} - ${currency.displayName}`;
      },
    },

    computed: {
      ...mapState({
        currencies: state => state.app.currencies,
        defaultCurrency: state => state.workspaces.currentWorkspace.defaultCurrency,
      }),

      currenciesData() {
        return currencies => currencies.map(currency => ({
          code: currency.code,
          symbol: i18n.getCurrencySymbol(currency.code),
          displayName: i18n.getCurrencyDisplayName(currency.code),
        }));
      },

      currenciesGroups() {
        const groups = [];
        if (this.currenciesShortlist.length > 0) {
          // todo #6: i18n
          groups.push({
            title: 'Recently Used Currencies',
            currencies: this.currenciesShortlist.map(it => ({ code: it })),
          });
        }
        groups.push({
          title: 'All Currencies',
          currencies: this.currencies,
        });
        return groups;
      },
    },

    watch: {
      value(val) {
        this.inputValue = val;
      },

      inputValue(val) {
        this.$emit('input', val);
      },
    },
  };
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

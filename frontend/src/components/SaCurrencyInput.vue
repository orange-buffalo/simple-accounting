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
        v-for="currency in group.currencies"
        :key="currency.code"
        :label="$t('saCurrencyInput.currencyLabel', currency)"
        :value="currency.code"
      >
        <div class="sa-currency-input">
          <span class="sa-currency-input__currency-code">{{ currency.code }}</span>
          <span class="sa-currency-input__currency-name">{{ currency.displayName }}</span>
        </div>
      </ElOption>
    </ElOptionGroup>
  </ElSelect>
</template>

<script>
  import { api } from '@/services/api';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import i18n from '@/services/i18n';

  export default {
    name: 'SaCurrencyInput',

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
        currencies: i18n.getCurrenciesInfo(),
      };
    },

    computed: {
      currenciesGroups() {
        const groups = [];
        if (this.currenciesShortlist.length > 0) {
          groups.push({
            title: this.$t('saCurrencyInput.groups.recent'),
            currencies: this.currenciesShortlist,
          });
        }
        groups.push({
          title: this.$t('saCurrencyInput.groups.all'),
          currencies: Object.values(this.currencies),
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

    async created() {
      if (!this.inputValue) {
        this.inputValue = this.defaultCurrency;
      }

      const currenciesShortlistResponse = await api
        .get(`/workspaces/${this.currentWorkspace.id}/statistics/currencies-shortlist`);
      this.currenciesShortlist = currenciesShortlistResponse.data
        .map((currencyCode) => this.currencies[currencyCode]);
    },
  };
</script>

<style lang="scss">
  .sa-currency-input {

    &__currency-code {
      font-size: 110%;
      display: inline-block;
      min-width: 3em;
    }

    &__currency-name {
      display: inline-block;
      margin-left: 5px;
      color: #6e716f;
      font-size: 90%;
    }
  }
</style>

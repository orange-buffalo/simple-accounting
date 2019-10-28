import { mapGetters } from 'vuex';

export const withCurrencyInfo = {
  computed: {
    currencySymbol() {
      return (currency) => {
        const currencyInfo = this.$_withCurrencyInfo_getCurrencyInfo(currency);
        return currencyInfo ? currencyInfo.symbol : '';
      };
    },

    currencyDisplayName() {
      return (currency) => {
        const currencyInfo = this.$_withCurrencyInfo_getCurrencyInfo(currency);
        return currencyInfo ? currencyInfo.displayName : '';
      };
    },

    currencyDigits() {
      return (currency) => {
        const currencyInfo = this.$_withCurrencyInfo_getCurrencyInfo(currency);
        return currencyInfo ? (currencyInfo._digits ? currencyInfo._digits : 2) : 2;
      };
    },

    ...mapGetters({
      $_withCurrencyInfo_getCurrencyInfo: 'i18n/getCurrencyInfo',
    }),
  },
};

export default withCurrencyInfo;

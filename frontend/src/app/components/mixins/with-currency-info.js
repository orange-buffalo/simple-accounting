import {mapGetters} from 'vuex'

export const withCurrencyInfo = {
  computed: {
    currencySymbol: function () {
      return currency => {
        let currencyInfo = this.$_withCurrencyInfo_getCurrencyInfo(currency)
        return currencyInfo ? currencyInfo.symbol : ''
      }
    },

    currencyDisplayName: function () {
      return currency => {
        let currencyInfo = this.$_withCurrencyInfo_getCurrencyInfo(currency)
        return currencyInfo ? currencyInfo.displayName : ''
      }
    },

    currencyDigits: function () {
      return currency => {
        let currencyInfo = this.$_withCurrencyInfo_getCurrencyInfo(currency)
        return currencyInfo ? (currencyInfo._digits ? currencyInfo._digits : 2) : 2
      }
    },

    ...mapGetters({
      $_withCurrencyInfo_getCurrencyInfo: 'i18n/getCurrencyInfo'
    })
  }
}

export default withCurrencyInfo
import {mapState} from 'vuex'

export const withNumberFormatter = {
  computed: {
    decimalSeparator: function () {
      return this.$_withNumberFormatter_numbersInfo ? this.$_withNumberFormatter_numbersInfo.decimal : ''
    },

    thousandSeparator: function () {
      return this.$_withNumberFormatter_numbersInfo ? this.$_withNumberFormatter_numbersInfo.group : ''
    },

    ...mapState('i18n', {
      $_withNumberFormatter_numbersInfo: 'numbersInfo',
      $_withNumberFormatter_defaultNumberParser: 'defaultNumberParser',
      $_withNumberFormatter_defaultNumberFormatter: 'defaultNumberFormatter'
    })
  },

  methods: {
    parserNumberDefault: function (input) {
      if (isNaN(input) && this.$_withNumberFormatter_defaultNumberParser) {
        return this.$_withNumberFormatter_defaultNumberParser(input)
      } else {
        return input
      }
    },

    formatNumberDefault: function (input) {
      if (isNaN(input)) {
        return input
      } else if (!this.$_withNumberFormatter_defaultNumberFormatter) {
        return input.toString()
      } else {
        return this.$_withNumberFormatter_defaultNumberFormatter(input)
      }
    }
  }
}

export default withNumberFormatter
import { mapState } from 'vuex';

export const withNumberFormatter = {
  computed: {
    decimalSeparator() {
      return this.$_withNumberFormatter_numbersInfo ? this.$_withNumberFormatter_numbersInfo.decimal : '';
    },

    thousandSeparator() {
      return this.$_withNumberFormatter_numbersInfo ? this.$_withNumberFormatter_numbersInfo.group : '';
    },

    ...mapState('i18n', {
      $_withNumberFormatter_numbersInfo: 'numbersInfo',
      $_withNumberFormatter_defaultNumberParser: 'defaultNumberParser',
      $_withNumberFormatter_defaultNumberFormatter: 'defaultNumberFormatter',
    }),
  },

  methods: {
    parserNumberDefault(input) {
      if (isNaN(input) && this.$_withNumberFormatter_defaultNumberParser) {
        return this.$_withNumberFormatter_defaultNumberParser(input);
      }
      return input;
    },

    formatNumberDefault(input) {
      if (isNaN(input)) {
        return input;
      } if (!this.$_withNumberFormatter_defaultNumberFormatter) {
        return input.toString();
      }
      return this.$_withNumberFormatter_defaultNumberFormatter(input);
    },
  },
};

export default withNumberFormatter;

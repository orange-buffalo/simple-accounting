import { mapState } from 'vuex';

export const withNumberFormatter = {
  computed: {

    ...mapState('i18n', {
      $_withNumberFormatter_defaultNumberFormatter: 'defaultNumberFormatter',
    }),
  },

  methods: {
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

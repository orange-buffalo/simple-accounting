import { assign } from 'lodash';
import merge from 'deepmerge';

let globalize;
let cldr;

function emptyFormatter() {
  return '';
}

function ensureFormatter(formatter, globalizeFormatterProvider) {
  if (!formatter || formatter === emptyFormatter) {
    return globalize ? globalizeFormatterProvider() : emptyFormatter;
  }
  return formatter;
}

export const i18nStore = {
  namespaced: true,

  state: {
    currencyFormatters: {},
    mediumDateFormatter: null,
    mediumDateTimeFormatter: null,
    currencyInfo: [],
    numbersInfo: null,
    defaultNumberParser: null,
  },

  // todo #6: perhaps use mutation instead of direct manipulation with state to have tooling support
  mutations: {},

  actions: {
    loadLocaleData({ state, dispatch }) {
      // todo #6: move to a separate js, can be loaded without splitting
      // todo #6: based on current locale
      import('cldrjs').then((cldrjs) => {
        import('globalize').then((globalizejs) => {
          import('./i18n/en-AU.cldr-data').then((module) => {
            cldrjs.default.load(module.default);
            globalizejs.default.load(module.default);
            cldr = new cldrjs.default('en-AU');
            globalize = globalizejs.default('en-AU');

            // todo #6: no need in lazy initialization for these formatters
            if (state.mediumDateFormatter) {
              dispatch('ensureMediumDateFormatter');
            }

            if (state.mediumDateTimeFormatter) {
              dispatch('ensureMediumDateTimeFormatter');
            }

            for (const currency in state.currencyFormatters) {
              if (state.currencyFormatters.hasOwnProperty(currency)) {
                dispatch('ensureCurrencyFormatter', currency);
              }
            }

            state.currencyInfo = merge(
              cldr.get('/main/{bundle}/numbers/currencies'),
              cldr.get('/supplemental/currencyData/fractions'),
            );

            state.numbersInfo = cldr.get('/main/{bundle}/numbers/symbols-numberSystem-latn');

            state.defaultNumberParser = globalize.numberParser();
            state.defaultNumberFormatter = globalize.numberFormatter();
          });
        });
      });
    },

    ensureCurrencyFormatter({ state }, currency) {
      const currencyFormatter = {};
      currencyFormatter[currency] = ensureFormatter(
        state.currencyFormatters[currency],
        () => globalize.currencyFormatter(currency),
      );
      state.currencyFormatters = assign({}, state.currencyFormatters, currencyFormatter);
    },

    ensureMediumDateFormatter({ state }) {
      state.mediumDateFormatter = ensureFormatter(
        state.mediumDateFormatter,
        () => globalize.dateFormatter({ date: 'medium' }),
      );
    },

    ensureMediumDateTimeFormatter({ state }) {
      state.mediumDateTimeFormatter = ensureFormatter(
        state.mediumDateTimeFormatter,
        () => globalize.dateFormatter({ skeleton: 'yMMMdhm' }),
      );
    },
  },

  getters: {
    getCurrencyInfo: state => (currency) => {
      const currencyInfo = state.currencyInfo[currency];
      return currencyInfo || {};
    },

    getCurrencyFormatter: state => currency => state.currencyFormatters[currency],
  },
};

export default i18nStore;

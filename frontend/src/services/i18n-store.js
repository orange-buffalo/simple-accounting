let globalize;

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
    mediumDateFormatter: null,
  },

  // todo #6: perhaps use mutation instead of direct manipulation with state to have tooling support
  mutations: {},

  actions: {
    async loadLocaleData({ state, dispatch }) {
      // todo #6: move to a separate js, can be loaded without splitting
      // todo #6: based on current locale
      const cldrjs = await import('cldrjs');
      const globalizejs = await import('globalize');
      const module = await import('@/i18n/l10n/en-AU.cldr-data');

      cldrjs.default.load(module.default);
      globalizejs.default.load(module.default);
      globalize = globalizejs.default('en-AU');

      // todo #6: no need in lazy initialization for these formatters
      if (state.mediumDateFormatter) {
        dispatch('ensureMediumDateFormatter');
      }
    },

    ensureMediumDateFormatter({ state }) {
      state.mediumDateFormatter = ensureFormatter(
        state.mediumDateFormatter,
        () => globalize.dateFormatter({ date: 'medium' }),
      );
    },
  },
};

export default i18nStore;

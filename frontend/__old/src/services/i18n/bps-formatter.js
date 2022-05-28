export default function bpsFormatter({ i18n }) {
  return function bpsFormatterImpl(bpsValue, locale, arg) {
    if (arg !== 'percent') {
      throw Error(`${arg} is not supported for bps formatter`);
    }
    if (bpsValue == null) {
      return null;
    }
    return i18n.t('common.percent', [bpsValue / 10000]);
  };
}

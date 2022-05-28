export default function yesNoFormatter({ i18n }) {
  return function yesNoFormatterImpl(booleanValue) {
    if (booleanValue) {
      return i18n.t('common.yesNo.yes');
    }
    return i18n.t('common.yesNo.no');
  };
}

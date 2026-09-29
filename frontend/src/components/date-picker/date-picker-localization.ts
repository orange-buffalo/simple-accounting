import { computed, ref } from 'vue';
import en from 'element-plus/es/locale/lang/en';
import uk from 'element-plus/es/locale/lang/uk';
import 'dayjs/locale/uk';

const locale = ref('en');

export function setDatePickerLocale(nextLocale: string) {
  locale.value = nextLocale;
}

export const elementPlusLocale = computed(() => (locale.value.startsWith('uk') ? uk : en));

export const datePickerFormat = computed(() => {
  const parts = new Intl.DateTimeFormat(locale.value, {
    year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date(1999, 2, 28));
  return parts.map((part) => {
    if (part.type === 'year') return 'YYYY';
    if (part.type === 'month') return 'MM';
    if (part.type === 'day') return 'DD';
    return part.value;
  }).join('');
});

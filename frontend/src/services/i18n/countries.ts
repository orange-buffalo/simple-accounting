import { getCurrentLanguage } from '@/services/i18n/i18n-services';

export function getCountryName(code: string): string {
  return new Intl.DisplayNames([getCurrentLanguage()], { type: 'region' }).of(code) ?? code;
}

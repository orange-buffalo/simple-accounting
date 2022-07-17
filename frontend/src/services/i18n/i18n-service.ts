import MessageFormat from '@messageformat/core';
import type { MessageFunction } from '@messageformat/core';
import { flatten } from 'flat';
import { lookupClosestLocale } from '@/services/i18n/locale-utils';
import supportedLocaleCodesJson from '@/services/i18n/l10n/supported-locales.json?raw';
import translationFilesDeferred from '@/services/i18n/t9n';
import { updateLocale } from '@/services/i18n/cldr-data';
import {
  amountFormatter,
  bpsFormatter,
  dateFormatter, dateTimeFormatter,
  fileSizeFormatter,
  yesNoFormatter,
} from '@/services/i18n/formatters';

// just a workaround for Typescript server error on importing json objects
const supportedLocaleCodes = JSON.parse(supportedLocaleCodesJson);

export interface SupportedLocale {
  locale: string,
  displayName: string,
}

export interface SupportedLanguage {
  languageCode: string,
  displayName: string,
}

const supportedLanguages: SupportedLanguage[] = [{
  languageCode: 'en',
  displayName: 'English',
}, {
  languageCode: 'uk',
  displayName: 'Українська',
}];

// function isVNode(node) {
//   return node !== null && typeof node === 'object' && Object.prototype.hasOwnProperty.call(node, 'componentOptions');
// }
//
// // messageformat does not (obviously) support vue component, so component interpolation does not work
// function shouldDelegateToDefaultFormatter(values) {
//   if (values != null) {
//     return Object.values(values)
//       .find((it) => it != null && (isVNode(it) || Object.values(it)
//         .find((nested) => isVNode(nested))));
//   }
//   return false;
// }

function getValidLocale(requestedLocales: readonly string[]) {
  return requestedLocales
    .map((requestedLocale) => lookupClosestLocale(requestedLocale, supportedLocaleCodes))
    .find((closestSupportedLocale) => closestSupportedLocale != null) || 'en';
}

function getValidLanguage(requestedLocales: readonly string[]) {
  const supportedLanguageCodes = supportedLanguages.map((it) => it.languageCode);
  return requestedLocales
    .map((requestedLocale) => lookupClosestLocale(requestedLocale, supportedLanguageCodes))
    .find((closestSupportedLocale) => closestSupportedLocale != null) || 'en';
}

export class I18nService {
  private currentLocale: string | null = null;

  private currentLanguage: string | null = null;

  private supportedLocales: SupportedLocale[] = [];

  private currentMessages: Record<string, string> = {};

  private formatter: MessageFormat | null = null;

  private messageCache: Record<string, MessageFunction<'string' | 'values'>> = {};

  private async loadLanguage(language: string) {
    const translations = await translationFilesDeferred[language]();
    this.currentMessages = flatten(translations);
    this.currentLanguage = language;
    this.supportedLocales = (await import(`./l10n/locales-display-names-${language}.json`)).default;
  }

  private async loadLocale(locale: string) {
    await updateLocale(locale);
    this.currentLocale = locale;
  }

  private initializeFormatter() {
    if (!this.currentLocale || !this.currentLanguage) throw new Error('Invalid state');

    this.formatter = new MessageFormat(this.currentLocale, {
      customFormatters: {
        amount: amountFormatter(),
        date: dateFormatter(),
        fileSize: fileSizeFormatter(),
        yesNo: yesNoFormatter(),
        bps: bpsFormatter(),
        // // todo #206: rename when https://github.com/messageformat/messageformat/issues/274 is resolved
        saDateTime: dateTimeFormatter(),
      },
    });
    this.messageCache = {};
  }

  private async setupI18n(locale: string, language: string) {
    let loadLocaleDeferred;
    if (this.currentLocale !== locale) {
      loadLocaleDeferred = this.loadLocale(locale);
    }

    let loadLanguageDeferred;
    if (this.currentLanguage !== language) {
      loadLanguageDeferred = this.loadLanguage(language);
    }

    await Promise.all([loadLocaleDeferred, loadLanguageDeferred]);

    this.initializeFormatter();
  }

  getSupportedLocales(): SupportedLocale[] {
    if (!this.supportedLocales.length) throw new Error('i18n has not been initialized');
    return this.supportedLocales;
  }

  static getSupportedLanguages(): SupportedLanguage[] {
    return supportedLanguages;
  }

  async setLocaleFromBrowser(): Promise<void> {
    await this.setupI18n(
      getValidLocale(navigator.languages),
      getValidLanguage(navigator.languages),
    );
  }

  async setLocaleFromProfile(locale: string, language: string): Promise<void> {
    await this.setupI18n(
      getValidLocale([I18nService.localeIdToLanguageTag(locale)]),
      getValidLanguage([I18nService.localeIdToLanguageTag(language)]),
    );
  }

  getCurrentLocale(): string {
    if (!this.currentLocale) throw new Error('i18n has not been initialized');
    return this.currentLocale;
  }

  getCurrentLanguage(): string {
    if (!this.currentLanguage) throw new Error('i18n has not been initialized');
    return this.currentLanguage;
  }

  static localeIdToLanguageTag(localeId: string): string {
    return localeId.replace(/_/g, '-');
  }

  t(messageKey: string, values?: Record<string, unknown> | unknown[]): string {
    if (!this.formatter) throw new Error('i18n has not been initialized');
    const message = this.currentMessages[messageKey];
    if (!message) return messageKey;

    // if (shouldDelegateToDefaultFormatter(values)) {
    //   return null;
    // }

    let messageFunction = this.messageCache[message];
    if (!messageFunction) {
      messageFunction = this.formatter.compile(message);
      this.messageCache[message] = messageFunction;
    }
    return messageFunction(values) as string;
  }
}

export const i18n = new I18nService();

// i18n.languageTagToLocaleId = function languageTagToLocaleId(localeId) {
//   return localeId.replace(/-/g, '_');
// };

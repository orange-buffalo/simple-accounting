import type { MessageFunction } from '@messageformat/core';
import MessageFormat from '@messageformat/core';
import { computed, ref } from 'vue';
import { lookupClosestLocale } from '@/services/i18n/locale-utils';
import supportedLocaleCodesJson from '@/services/i18n/l10n/supported-locales.json?raw';
import type { Translations } from '@/services/i18n/t9n';
import translationFilesDeferred from '@/services/i18n/t9n';
import { updateLocale } from '@/services/i18n/cldr-data';
import {
  amountFormatter,
  bpsFormatter,
  dateFormatter,
  dateTimeFormatter,
  fileSizeFormatter,
  yesNoFormatter,
} from '@/services/i18n/formatters';

import { setTranslationsFormatter } from '@/services/i18n/t9n/formatter';

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

let currentLocale: string | null = null;

let currentLanguage: string | null = null;

let supportedLocales: SupportedLocale[] = [];

let formatter: MessageFormat | null = null;

let messageCache: Record<string, MessageFunction<'string' | 'values'>> = {};

const currentTranslationsRef = ref<Translations | null>(null);

async function loadLanguage(language: string) {
  currentTranslationsRef.value = await translationFilesDeferred[language]();
  currentLanguage = language;
  supportedLocales = (await import(`./l10n/locales-display-names-${language}.json`)).default;
  // eslint-disable-next-line arrow-body-style
  setTranslationsFormatter((messageTemplate: string, values?: Record<string, unknown> | unknown[]) => {
    return formatMessage(messageTemplate, values);
  });
}

async function loadLocale(locale: string) {
  await updateLocale(locale);
  currentLocale = locale;
}

function initializeFormatter() {
  if (!currentLocale || !currentLanguage) throw new Error('Invalid state');

  formatter = new MessageFormat(currentLocale, {
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
  messageCache = {};
}

async function setupI18n(locale: string, language: string) {
  let loadLocaleDeferred;
  if (currentLocale !== locale) {
    loadLocaleDeferred = loadLocale(locale);
  }

  let loadLanguageDeferred;
  if (currentLanguage !== language) {
    loadLanguageDeferred = loadLanguage(language);
  }

  await Promise.all([loadLocaleDeferred, loadLanguageDeferred]);

  initializeFormatter();
}

export function getSupportedLocales(): SupportedLocale[] {
  if (!supportedLocales.length) throw new Error('i18n has not been initialized');
  return supportedLocales;
}

export function getSupportedLanguages(): SupportedLanguage[] {
  return supportedLanguages;
}

export async function setLocaleFromBrowser(): Promise<void> {
  await setupI18n(
    getValidLocale(navigator.languages),
    getValidLanguage(navigator.languages),
  );
}

export async function setLocaleFromProfile(locale: string, language: string): Promise<void> {
  await setupI18n(
    getValidLocale([localeIdToLanguageTag(locale)]),
    getValidLanguage([localeIdToLanguageTag(language)]),
  );
}

export function getCurrentLocale(): string {
  if (!currentLocale) throw new Error('i18n has not been initialized');
  return currentLocale;
}

export function getCurrentLanguage(): string {
  if (!currentLanguage) throw new Error('i18n has not been initialized');
  return currentLanguage;
}

function localeIdToLanguageTag(localeId: string): string {
  return localeId.replace(/_/g, '-');
}

export function formatMessage(messageTemplate: string, values?: Record<string, unknown> | unknown[]): string {
  if (!formatter) throw new Error('i18n has not been initialized');

  // if (shouldDelegateToDefaultFormatter(values)) {
  //   return null;
  // }

  let messageFunction = messageCache[messageTemplate];
  if (!messageFunction) {
    messageFunction = formatter.compile(messageTemplate);
    messageCache[messageTemplate] = messageFunction;
  }
  return messageFunction(values) as string;
}

export const $t = computed<Translations>(() => {
  const translations = currentTranslationsRef.value;
  if (!translations) throw new Error('i18n is not yet initialized');
  return translations;
});

// i18n.languageTagToLocaleId = function languageTagToLocaleId(localeId) {
//   return localeId.replace(/-/g, '_');
// };

import Cldr from 'cldrjs';
import fs from 'fs';
import path from 'path';

function getMergedCldrJson(baseDir, jsonFiles) {
  const mergedJson = jsonFiles.reduce((totalJson, jsonFileName) => {
    const jsonFilePath = `${baseDir}/${jsonFileName}.json`;
    const jsonFile = path.resolve(jsonFilePath);

    const jsonContent = fs.readFileSync(jsonFile, 'utf-8');

    return totalJson ? `${totalJson}, ${jsonContent}` : jsonContent;
  }, '');
  return JSON.stringify(JSON.parse(`[${mergedJson}]`));
}

function mergeAndSaveCldrJsonFiles(baseDir, jsonFiles, outputFile) {
  const mergedJson = getMergedCldrJson(baseDir, jsonFiles);
  fs.writeFileSync(outputFile, mergedJson);
}

const baseCodeGenDir = 'src/services/i18n/l10n';
const baseCldrDataDir = import.meta.resolve('cldr-data').replace('file://', '').replace('index.js', '');

function prepareCodeGenDir() {
  if (!fs.existsSync(baseCodeGenDir)) {
    fs.mkdirSync(baseCodeGenDir, { recursive: true });
  }
}

function generateBaseLocalBundleJson() {
  mergeAndSaveCldrJsonFiles(
    `${baseCldrDataDir}/supplemental`,
    ['metaZones', 'timeData', 'weekData', 'numberingSystems', 'currencyData', 'likelySubtags', 'plurals'],
    `${baseCodeGenDir}/base.json`,
  );
}

function getSupportedLocalesCodes() {
  const baseLocalesDir = `${baseCldrDataDir}/main`;
  const localesDirectories = fs.readdirSync(baseLocalesDir);
  return {
    baseLocalesDir,
    localesCodes: localesDirectories,
  };
}

function generateLocalesBundlesJsons() {
  const { baseLocalesDir, localesCodes } = getSupportedLocalesCodes();
  localesCodes.forEach((locale) => {
    mergeAndSaveCldrJsonFiles(
      `${baseLocalesDir}/${locale}`,
      ['ca-gregorian', 'timeZoneNames', 'numbers', 'currencies', 'units'],
      `${baseCodeGenDir}/locale-${locale}.json`,
    );
  });
}

function generateLocalesDisplayNames() {
  const messagesDir = 'src/services/i18n/t9n';
  const messagesFiles = fs
    .readdirSync(messagesDir)
    .filter((it) => it !== 'index.ts')
    .filter((it) => it !== 'formatter.ts');

  const { localesCodes } = getSupportedLocalesCodes();

  messagesFiles.forEach((messagesFile) => {
    const languageRx = /(.*?)\.ts/g;
    const [, language] = languageRx.exec(messagesFile);

    const languageCldrJson = getMergedCldrJson(baseCldrDataDir, [
      'supplemental/likelySubtags',
      `main/${language}/languages`,
      `main/${language}/territories`,
      `main/${language}/variants`,
      `main/${language}/scripts`,
    ]);

    Cldr.load(JSON.parse(languageCldrJson));
    const cldr = new Cldr(language);

    const localizedLocales = localesCodes
      .map((localeCode) => ({
        locale: localeCode,
        displayName: cldr.main(`localeDisplayNames/languages/${localeCode}`),
      }))
      .map((supportedLocale) => {
        if (supportedLocale.displayName == null) {
          const localeCode = supportedLocale.locale;
          const [languageTag, ...otherTags] = localeCode.split('-');
          const localizedTags = otherTags
            .map((tag) => {
              let localizedTag = cldr.main(`localeDisplayNames/scripts/${tag}`);
              if (localizedTag == null) {
                localizedTag = cldr.main(`localeDisplayNames/territories/${tag}`);
              }
              if (localizedTag == null) {
                localizedTag = cldr.main(`localeDisplayNames/variants/${tag}`);
              }
              return localizedTag;
            })
            .join(', ');

          const localizedLanguage = cldr.main(`localeDisplayNames/languages/${languageTag}`);
          const displayName = localizedTags ? `${localizedLanguage} (${localizedTags})` : localizedLanguage;

          return {
            ...supportedLocale,
            displayName,
          };
        }
        return supportedLocale;
      })
      .map((supportedLocale) => {
        let { displayName } = supportedLocale;
        displayName = displayName[0].toLocaleUpperCase(language) + displayName.slice(1);
        return {
          ...supportedLocale,
          displayName,
        };
      });

    fs.writeFileSync(`${baseCodeGenDir}/locales-display-names-${language}.json`, JSON.stringify(localizedLocales));
  });
}

function generateSupportedLocales() {
  const { localesCodes } = getSupportedLocalesCodes();
  fs.writeFileSync(`${baseCodeGenDir}/supported-locales.json`, JSON.stringify(localesCodes));
}

export function generateLocaleBundles() {
  prepareCodeGenDir();
  generateBaseLocalBundleJson();
  generateLocalesBundlesJsons();
  generateLocalesDisplayNames();
  generateSupportedLocales();
}

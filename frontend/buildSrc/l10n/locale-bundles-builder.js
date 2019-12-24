const fs = require('fs');
const path = require('path');

function mergeJsonFiles(baseDir, jsonFiles, outputFile) {
  const mergedJson = jsonFiles.reduce((totalJson, jsonFileName) => {
    const jsonFilePath = `${baseDir}/${jsonFileName}.json`;
    const jsonFile = path.resolve(jsonFilePath);

    const jsonContent = fs.readFileSync(jsonFile, 'utf-8');

    return totalJson ? `${totalJson}, ${jsonContent}` : jsonContent;
  }, '');

  fs.writeFileSync(outputFile, `[${mergedJson}]`);
}

const baseCodeGenDir = 'src/i18n/l10n';
fs.mkdirSync(baseCodeGenDir, { recursive: true });

const baseCldrDataDir = 'node_modules/cldr-data';

mergeJsonFiles(
  `${baseCldrDataDir}/supplemental`,
  [
    'metaZones',
    'timeData',
    'weekData',
    'numberingSystems',
    'currencyData',
    'likelySubtags',
  ],
  `${baseCodeGenDir}/base.json`,
);

const baseLocalesDir = `${baseCldrDataDir}/main`;
const fileList = fs.readdirSync(baseLocalesDir);
const availableLocales = [];
fileList.forEach((locale) => {
  availableLocales.push(locale);

  mergeJsonFiles(
    `${baseLocalesDir}/${locale}`,
    [
      'ca-gregorian',
      'timeZoneNames',
      'numbers',
      'currencies',
      'languages',
    ],
    `${baseCodeGenDir}/${locale}.json`,
  );
});

fs.writeFileSync(`${baseCodeGenDir}/locales.json`, JSON.stringify(availableLocales));

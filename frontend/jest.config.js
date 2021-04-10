module.exports = {
  preset: '@vue/cli-plugin-unit-jest/presets/typescript-and-babel',
  setupFilesAfterEnv: ['jest-expect-message', 'jest-extended', 'jest-date-mock', 'jest-localstorage-mock'],
  collectCoverage: true,
};

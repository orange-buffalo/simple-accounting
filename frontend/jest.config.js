module.exports = {
  preset: '@vue/cli-plugin-unit-jest/presets/typescript-and-babel',
  setupFilesAfterEnv: ['jest-expect-message', 'jest-extended', 'jest-date-mock'],
  collectCoverage: true,
};

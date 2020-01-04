module.exports = {
  preset: '@vue/cli-plugin-unit-jest',
  setupFilesAfterEnv: ['jest-expect-message', 'jest-extended', 'jest-date-mock'],
};

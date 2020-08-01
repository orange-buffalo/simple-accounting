module.exports = {
  preset: '@vue/cli-plugin-unit-jest',
  testMatch: ['**/Storyshots.spec.js'],
  globalSetup: '<rootDir>/tests/storybook/utils/stories-docker-environment-setup',
  globalTeardown: '<rootDir>/tests/storybook/utils/stories-docker-environment-teardown',
  setupFilesAfterEnv: ['jest-date-mock'],
};

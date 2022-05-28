module.exports = {
  preset: '@vue/cli-plugin-unit-jest/presets/typescript-and-babel',
  testMatch: ['**/Storyshots.spec.js'],
  globalSetup: '<rootDir>/tests/storybook/utils/stories-docker-environment-setup',
  globalTeardown: '<rootDir>/tests/storybook/utils/stories-docker-environment-teardown',
  setupFilesAfterEnv: ['jest-date-mock'],
  transformIgnorePatterns: [
    // a workaround for "Unexpected token 'export'"
    'node_modules/(@marked)',
  ],
};

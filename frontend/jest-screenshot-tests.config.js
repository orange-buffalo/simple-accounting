module.exports = {
  preset: '@vue/cli-plugin-unit-jest',
  testMatch: ['**/Storyshots.spec.js'],
  setupFilesAfterEnv: ['jest-date-mock'],
};

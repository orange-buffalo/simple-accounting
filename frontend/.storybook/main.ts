import type { StorybookViteConfig } from '@storybook/builder-vite';
import type { ConfigEnv } from 'vite';
import { loadConfigFromFile, mergeConfig } from 'vite';

const config: StorybookViteConfig = {
  stories: [
    '../src/**/*.stories.@(js|jsx|ts|tsx)',
  ],
  addons: [],
  framework: '@storybook/vue3',
  core: {
    builder: '@storybook/builder-vite',
    disableTelemetry: true,
  },
  features: {
    storyStoreV7: true,
  },
  async viteFinal(config, { configType }) {
    // @ts-ignore
    const { config: userConfig } = await loadConfigFromFile(
      configType as any as ConfigEnv,
      require('path')
        .resolve(__dirname, '../vite.config.ts'),
    );

    return mergeConfig(config, {
      ...userConfig,
      // manually specify plugins to avoid conflict
      plugins: [],
    });
  },
};

module.exports = config;

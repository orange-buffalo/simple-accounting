import type { StorybookConfig } from '@storybook/vue3-vite';
import type { ConfigEnv } from 'vite';
import { loadConfigFromFile, mergeConfig } from 'vite';
import { resolve } from 'path';
import { vitePlugins } from '../vite-plugins';

const storybookConfig: StorybookConfig = {
  stories: ['../../src/**/*.stories.@(js|jsx|ts|tsx)'],
  addons: ['@storybook/addon-actions'],
  framework: {
    name: '@storybook/vue3-vite',
    options: {},
  },
  core: {
    disableTelemetry: true,
  },
  features: {
    storyStoreV7: true,
    buildStoriesJson: true,
  },
  async viteFinal(config, { configType }) {
    // @ts-ignore
    const { config: userConfig } = await loadConfigFromFile(
      /* eslint-disable @typescript-eslint/no-explicit-any */
      configType as any as ConfigEnv,
      // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
      resolve(__dirname, '../../vite.config.ts'),
    );

    return mergeConfig(config, {
      ...userConfig,
      // manually specify plugins to avoid conflict
      plugins: [...vitePlugins],
      // override as storybook uses 'outDir' from main config
      build: {
        outDir: '../build/storybook',
      },
    });
  },
};

module.exports = storybookConfig;

import type { StorybookViteConfig } from '@storybook/builder-vite';
import type { ConfigEnv } from 'vite';
import { loadConfigFromFile, mergeConfig } from 'vite';
import { vitePlugins } from '../vite-plugins';

const storybookConfig: StorybookViteConfig = {
  stories: [
    '../../src/**/*.stories.@(js|jsx|ts|tsx)',
  ],
  addons: [
    '@storybook/addon-actions',
  ],
  framework: '@storybook/vue3',
  core: {
    builder: '@storybook/builder-vite',
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
      require('path')
        .resolve(__dirname, '../../vite.config.ts'),
    );

    return mergeConfig(config, {
      ...userConfig,
      // manually specify plugins to avoid conflict
      plugins: [...vitePlugins],
    });
  },
};

module.exports = storybookConfig;

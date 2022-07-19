import { fileURLToPath, URL } from 'url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { visualizer } from 'rollup-plugin-visualizer';
import { vitePlugins } from './build-config/vite-plugins';

// https://vitejs.dev/config/
export default defineConfig({
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: '@use "@/styles/vars.scss" as *;',
      },
    },
  },
  plugins: [
    vue(),
    visualizer({
      filename: './build/bundle-stats.html',
      template: 'network',
    }),
    ...vitePlugins,
  ],
  resolve: {
    alias: {
      // cannot use import.meta.url, storybook fails
      '@': fileURLToPath(new URL('./src', `file://${__filename}`)),
    },
  },
  server: {
    proxy: {
      '^/api': {
        target: 'http://localhost:9393',
      },
    },
  },
  test: {
    environment: 'jsdom',
  },
  build: {
    sourcemap: 'inline',
  },
  optimizeDeps: {
    include: ['jwt-decode'],
  },
});

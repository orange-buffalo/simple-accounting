import { fileURLToPath, URL } from 'url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { visualizer } from 'rollup-plugin-visualizer';
import { vitePlugins } from './build-config/vite-plugins';
import { resolveProxyPort } from './build-config/proxy-port-resolver';

const apiProxyPort = resolveProxyPort();

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    visualizer({
      filename: './build/bundle-stats.html',
      template: 'sunburst',
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
        target: `http://localhost:${apiProxyPort}`,
      },
    },
  },
  test: {
    environment: 'jsdom',
  },
  build: {
    sourcemap: 'inline',
    outDir: 'dist/META-INF/resources',
    chunkSizeWarningLimit: 50000,
  },
  optimizeDeps: {
    include: ['jwt-decode'],
  },
  css: {
    preprocessorOptions: {
      scss: {
        quietDeps: true,
      },
    },
  },
});

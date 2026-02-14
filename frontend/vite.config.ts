import vue from '@vitejs/plugin-vue';
import { visualizer } from 'rollup-plugin-visualizer';
import { fileURLToPath, URL } from 'url';
import { defineConfig } from 'vite';
import { resolveProxyPort } from './build-config/proxy-port-resolver';
import { vitePlugins } from './build-config/vite-plugins';

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
    // Vitest 4.x is stricter about reporting unhandled errors. There's a known issue with fetch-mock 12.5.4
    // where it tries to call getReader() on locked ReadableStreams during cleanup when requests are aborted.
    // This only affects cleanup and doesn't impact test correctness. The issue is fixed in fetch-mock 12.6.0+
    // but upgrading is blocked by Bun compatibility issues.
    dangerouslyIgnoreUnhandledErrors: true,
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

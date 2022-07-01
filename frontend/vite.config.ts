import { fileURLToPath, URL } from 'url';
/// <reference types="vitest" />
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
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
    ...vitePlugins,
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
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

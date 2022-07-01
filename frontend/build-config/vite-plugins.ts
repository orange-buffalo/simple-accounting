import svgLoader from 'vite-svg-loader';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export const vitePlugins = [
  AutoImport({
    resolvers: [ElementPlusResolver()],
  }),
  Components({
    resolvers: [ElementPlusResolver({
      importStyle: 'sass',
    })],
  }),
  svgLoader({
    svgoConfig: {
      plugins: [{
        name: 'removeDimensions',
        active: true,
      }, {
        name: 'removeViewBox',
        active: false,
      }],
    },
  }),
];

import svgLoader from 'vite-svg-loader';

export const vitePlugins = [
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

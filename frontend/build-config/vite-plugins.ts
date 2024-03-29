import svgLoader from 'vite-svg-loader';

export const vitePlugins = [
  svgLoader({
    svgoConfig: {
      plugins: [
        {
          name: 'preset-default',
          params: {
            overrides: {
              removeViewBox: false,
            },
          },
        },
        'removeDimensions',
      ],
    },
  }),
];

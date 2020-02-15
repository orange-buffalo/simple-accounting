module.exports = {
  assetsDir: 'assets',

  lintOnSave: false,

  devServer: {
    port: 9091,
    proxy: 'http://localhost:9393',
    disableHostCheck: true,
  },

  configureWebpack: (config) => {
    config.performance = {
      hints: false,
    };
    config.devtool = 'source-map';
  },

  chainWebpack: (config) => {
    // workaround for globalize with webpack
    config
      .resolve
      .alias
      .set('cldr$', 'cldrjs')
      .set('cldr', 'cldrjs/dist/cldr')
      .end();

    config
      .entry('app')
      .clear();
    config
      .entry('app')
      .add('./src/entry.js');

    config.plugins.delete('prefetch');

    const svgRule = config.module.rule('svg');

    svgRule.uses.clear();

    svgRule
      .use('babel-loader')
      .loader('babel-loader')
      .end()
      .use('vue-svg-loader')
      .loader('vue-svg-loader')
      .options({
        svgo: {
          plugins: [{ removeDimensions: true }, { removeViewBox: false }],
        },
      });
  },

  pluginOptions: {
    webpackBundleAnalyzer: {
      openAnalyzer: false,
      analyzerMode: 'static',
    },
  },
};

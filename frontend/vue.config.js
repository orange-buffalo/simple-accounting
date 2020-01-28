module.exports = {
  assetsDir: 'assets',

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
  },

  pluginOptions: {
    webpackBundleAnalyzer: {
      openAnalyzer: false,
      analyzerMode: 'static',
    },
  },
};

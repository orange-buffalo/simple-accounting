module.exports = {
  pages: {
    admin: {
      entry: 'src/admin/main.js',
      template: 'public/index.html',
      filename: 'admin/index.html'
    },

    user: {
      entry: 'src/app/loader.js',
      template: 'public/index.html',
      filename: 'app/index.html'
    }
  },

  assetsDir: 'static',

  devServer: {
    historyApiFallback: {
      rewrites: [
        {from: /^\/admin/, to: '/admin/index.html'},
        {from: /^\/app/, to: '/app/index.html'},
        {from: /./, to: '/views/404.html'}
      ]
    },
    port: 9091,
    proxy: 'http://localhost:9393',
    disableHostCheck: true
  },

  chainWebpack: config => {
    // custom loader for CLDR data
    config.module
        .rule('cldr-data')
        .test(/\.cldr-data$/)
        .use('cldr-data')
        .loader(require('path').resolve('src/loaders/cldr-data-loader'))
        .end()

    // workaround for globalize with webpack
    config
        .resolve
        .alias
        .set('cldr$', 'cldrjs')
        .set('cldr', 'cldrjs/dist/cldr')
        .end()
  }
}
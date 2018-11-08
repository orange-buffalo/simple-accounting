module.exports = {
  pages: {
    admin: {
      entry: 'src/admin/main.js',
      template: 'public/index.html',
      filename: 'admin/index.html'
    },

    user: {
      entry: 'src/app/main.js',
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
    config.module
        .rule('cldr-data')
        .test(/\.cldr-data$/)
        .use('cldr-data')
        .loader(require('path').resolve('src/loaders/cldr-data-loader'))
        .end()
  }
}
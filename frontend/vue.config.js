module.exports = {
  pages: {
    admin: {
      entry: 'src/admin/main.js',
      template: 'public/index.html',
      filename: 'admin/index.html'
    },

    user: {
      entry: 'src/user/main.js',
      template: 'public/index.html',
      filename: 'user/index.html'
    },
  }   ,

  assetsDir: 'static',

  devServer: {
     historyApiFallback: {
      rewrites: [
        { from: /^\/admin/, to: '/admin/index.html' },
        { from: /^\/user/, to: '/user/index.html' },
        { from: /./, to: '/views/404.html' }
      ]
    } ,
    port: 9091
    // proxy: {
    //
    // }
  }
}
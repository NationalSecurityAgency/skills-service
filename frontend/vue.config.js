const path = require('path');

const resolve = dir => path.join(__dirname, dir);

const proxyConf = {
  target: 'http://localhost:8080',
  changeOrigin: true,
};

module.exports = {
  devServer: {
    host: 'localhost',
    port: 8082,
    overlay: true,
    proxy: {
      '/admin': proxyConf,
      '/app': proxyConf,
      '/api': proxyConf,
      '/server': proxyConf,
      '/icons': proxyConf,
      '/performLogin': proxyConf,
      '/logout': proxyConf,
      '/createAccount': proxyConf,
      '/oauth2': proxyConf,
      '/login': proxyConf,
      '/static': proxyConf,
    },
  },
  configureWebpack: {
    resolve: {
      alias: {
        '@$': resolve('src'),
      },
    },
    devtool: 'cheap-module-eval-source-map',
  },

  publicPath: './',
  outputDir: undefined,
  assetsDir: 'static',
  runtimeCompiler: true,
  productionSourceMap: undefined,
  parallel: undefined,
  css: undefined,
};

const path = require('path');

const resolve = dir => path.join(__dirname, dir);

module.exports = {
  devServer: {
    host: 'localhost',
    port: 8082,
    overlay: true,
    proxy: {
      '/': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
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

  publicPath: undefined,
  outputDir: undefined,
  assetsDir: undefined,
  runtimeCompiler: true,
  productionSourceMap: undefined,
  parallel: undefined,
  css: undefined,
};

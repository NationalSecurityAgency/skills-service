const path = require('path');

const resolve = dir => path.join(__dirname, dir);

module.exports = {
  configureWebpack: {
    resolve: {
      alias: {
        '@$': resolve('src'),
      },
    },
    devtool: 'cheap-module-eval-source-map',
  },

  outputDir: undefined,
  assetsDir: 'static',
  runtimeCompiler: true,
  productionSourceMap: undefined,
  parallel: undefined,
  css: undefined,
};

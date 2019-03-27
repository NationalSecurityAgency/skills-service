const merge = require('webpack-merge');
const fs = require('fs');

console.log(process.env.USER_SKILLS_WEBPACK_DEV_SERVER_SSL_KEY_PATH);
console.log(process.env.USER_SKILLS_WEBPACK_DEV_SERVER_SSL_CERT_PATH);

let exportObject = {
  configureWebpack: {
    optimization: {
      // We no not want to minimize our code.
      minimize: false
    },
  },
  publicPath: '.',
  runtimeCompiler: true,
  assetsDir: 'static',
};

if (!process.env.USER_SKILLS_WEBPACK_DEV_SERVER_SSL_KEY_PATH || !process.env.USER_SKILLS_WEBPACK_DEV_SERVER_SSL_CERT_PATH) {
  console.warn('No SSL certificates configured')
  module.exports = exportObject;
} else {
  exportObject = merge(exportObject, {
    devServer: {
      https: {
        key: fs.readFileSync(process.env.USER_SKILLS_WEBPACK_DEV_SERVER_SSL_KEY_PATH),
        cert: fs.readFileSync(process.env.USER_SKILLS_WEBPACK_DEV_SERVER_SSL_CERT_PATH),
      },
    }
  });
}

module.exports = exportObject;

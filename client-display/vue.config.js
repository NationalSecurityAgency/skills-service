const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const plugins = [];
// uncomment to add bundle analysis of production build
// plugins.push(new BundleAnalyzerPlugin());

const exportObject = {
  configureWebpack: {
    plugins,
  },
};

const proxyConf = {
  target: 'http://localhost:8080',
  changeOrigin: true,
};

if (process.env.NODE_ENV === 'production') {
  console.log('production mode detected');
  exportObject.publicPath = '.';
}

exportObject.devServer = {
  host: 'localhost',
  port: 8083,
  overlay: true,
  proxy: {
    '/admin': proxyConf,
  },
};

module.exports = exportObject;

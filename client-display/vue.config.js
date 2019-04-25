const exportObject = {
  configureWebpack: {
  },
};

const proxyConf = {
  target: 'http://localhost:8080',
  changeOrigin: true,
};

if (process.env.NODE_ENV === 'production') {
  console.log('production mode detected');
  exportObject.publicPath = '.'
}

exportObject.devServer = {
  host: 'localhost',
  port: 8083,
  overlay: true,
  proxy: {
    '/admin': proxyConf,
  }
};

module.exports = exportObject;

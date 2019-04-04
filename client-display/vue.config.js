const exportObject = {
  configureWebpack: {
  },
};

if (process.env.NODE_ENV === 'production') {
  console.log('production mode detected');
  exportObject.publicPath = '.'
}

module.exports = exportObject;

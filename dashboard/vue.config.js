const path = require('path');
const fs = require('fs');

const resolve = dir => path.join(__dirname, dir);

const getTarget = () => {
  let target = 'http://localhost:8080';
  const proxySslKeyLocation = process.env.WEBPACK_DEV_SERVER_PROXY_TABLE_SSL_KEY_PATH;
  const proxyCertKeyLocation = process.env.WEBPACK_DEV_SERVER_PROXY_TABLE_SSL_CERT_PATH;
  if (fs.existsSync(proxySslKeyLocation) && fs.existsSync(proxyCertKeyLocation)) {
    target = {
      host: 'localhost',
      port: 8443,
      key: fs.readFileSync(proxySslKeyLocation),
      cert: fs.readFileSync(proxyCertKeyLocation),
      protocol: 'https:'
    };
  }
  return target;
};

const proxyConf = {
  target: getTarget(),
  secure: false,
  changeOrigin: true,
  logLevel: 'debug',
};

// absolute paths to all symlinked modules inside `nodeModulesPath`
// adapted from https://github.com/webpack/webpack/issues/811#issuecomment-405199263
const findLinkedModules = (nodeModulesPath) => {
  const modules = []

  fs.readdirSync(nodeModulesPath).forEach(dirname => {
    const modulePath = path.resolve(nodeModulesPath, dirname)
    const stat = fs.lstatSync(modulePath)

    if (dirname.startsWith('.')) {
      // not a module or scope, ignore
    } else if (dirname.startsWith('@')) {
      // scoped modules
      modules.push(...findLinkedModules(modulePath))
    } else if (stat.isSymbolicLink()) {
      const realPath = fs.realpathSync(modulePath)
      const realModulePath = path.resolve(realPath, 'node_modules')

      modules.push(realModulePath)
    }
  })

  return modules
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
      '/grantFirstRoot': proxyConf,
      '/createRootAccount': proxyConf,
      '/oauth2': proxyConf,
      '/login': proxyConf,
      '/static': proxyConf,
      '/root': proxyConf,
      '/userExists': proxyConf,
      '/supervisor': proxyConf,
      '/public': proxyConf,
      '/metrics' : proxyConf,
      '/skills-websocket' : proxyConf,
      '/resetPassword' : proxyConf,
      '/performPasswordReset' : proxyConf,
      '/isFeatureSupported' : proxyConf,
    },
  },
  configureWebpack: {
    resolve: {
      alias: {
        '@$': resolve('src'),
      },
      // work with npm link
      // see https://github.com/webpack/webpack/issues/985
      // see https://github.com/vuejs-templates/webpack/pull/688
      symlinks: false,
      modules: [
        // provide absolute path to the main node_modules,
        // to avoid webpack searching around and getting confused
        // see https://webpack.js.org/configuration/resolve/#resolve-modules
        path.resolve('node_modules'),
        // include linked node_modules as fallback, in case the deps haven't
        // yet propagated to the main node_modules
        ...findLinkedModules(path.resolve('node_modules')),
      ],
    },
    devtool: 'eval-nosources-source-map',
    // optimization: { minimize: false },
  },

  outputDir: undefined,
  assetsDir: 'static',
  runtimeCompiler: true,
  productionSourceMap: undefined,
  parallel: undefined,
  css: undefined,
};

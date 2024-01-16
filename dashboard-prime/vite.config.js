import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// TODO need to add support for PKI mode
const proxyConf = {
  target: 'http://localhost:8080',
  changeOrigin: true,
};

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
    preserveSymlinks: true
  },
  server: {
    proxy: {
      '^/admin/': proxyConf,
      '^/app/': proxyConf,
      '^/api/': proxyConf,
      '^/server$': proxyConf,
      '^/icons$': proxyConf,
      '^/performLogin$': proxyConf,
      '^/logout$': proxyConf,
      '^/createAccount$': proxyConf,
      '^/grantFirstRoot$': proxyConf,
      '^/createRootAccount$': proxyConf,
      '^/oauth2$': proxyConf,
      '^/login$': proxyConf,
      '^/static/': proxyConf,
      '^/root/': proxyConf,
      '^/userExists': proxyConf,
      '^/supervisor/': proxyConf,
      '^/public/': proxyConf,
      '^/metrics/' : proxyConf,
      '^/skills-websocket/' : proxyConf,
      '^/resetPassword$' : proxyConf,
      '^/performPasswordReset$' : proxyConf,
      '^/isFeatureSupported$' : proxyConf,
      '^/resendEmailVerification/' : proxyConf,
      '^/verifyEmail$' : proxyConf,
      '^/userEmailIsVerified/' : proxyConf,
    },
  }
})

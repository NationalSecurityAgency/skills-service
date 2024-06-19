/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
      '^/root/': proxyConf,
      '^/userExists': proxyConf,
      '^/supervisor/': proxyConf,
      '^/public/': proxyConf,
      '^/metrics/' : proxyConf,
      '^/resetPassword$' : proxyConf,
      '^/performPasswordReset$' : proxyConf,
      '^/isFeatureSupported$' : proxyConf,
      '^/resendEmailVerification/' : proxyConf,
      '^/verifyEmail$' : proxyConf,
      '^/userEmailIsVerified/' : proxyConf,
      '^/skills-websocket/info' : proxyConf,
      '^/skills-websocket/.*/websocket$' : {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  }
})

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
import { visualizer } from 'rollup-plugin-visualizer'

// TODO need to add support for PKI mode
const proxyConf = {
  target: 'http://localhost:8080',
  changeOrigin: true,
};

function getChunkId(id) {

    if (id.includes('apex')) {
        return 'apexCharts';
    } else if (id.includes('@primeuix')) {
        return 'primeuix';
    } else if (id.includes('primevue')) {
        return 'primevue';
    }  else if (id.includes('@toast-ui/editor') || id.includes('toastui-editor-viewer')) {
        return 'toastUI';
    }else if (id.includes('@tsparticles')
        || id.includes('axios')
        || id.includes('yup')
        || id.includes('@skilltree/skills-client-js')
        || id.includes('vee-validate')
        || id.includes('tinycolor2')
        || id.includes('vue-router')
        || id.includes('sortablejs')
        || id.includes('dompurify')
        || id.includes('webvtt-parser')
        || id.includes('dayjs')
        || id.includes('@vueuse')
    ) {
        return 'support-libs';
    }

    return null
}

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
      '^/openai/': proxyConf,
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
  },
  build: {
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks: getChunkId
      },
      plugins: [
        visualizer({
          open: false,
        }),
      ]
    }
  }
})


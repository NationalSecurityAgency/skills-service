const { defineConfig } = require('cypress')

module.exports = defineConfig({
  projectId: '7kivjf',
  requestTimeout: 10000,
  defaultCommandTimeout: 10000,
  numTestsKeptInMemory: 50,
  chromeWebSecurity: false,
  retries: {
    runMode: 2,
    openMode: 0,
  },
  lighthouse: {
    performance: 85,
    accessibility: 90,
    'best-practices': 85,
    seo: 50,
    pwa: 50,
  },
  e2e: {
    // We've imported your old cypress plugins here.
    // You may want to clean this up later by importing these.
    setupNodeEvents(on, config) {
      return require('./cypress/plugins/index.js')(on, config)
    },
    baseUrl: 'http://localhost:8080',
    specPattern: 'cypress/e2e/**/*.{js,jsx,ts,tsx}',
  },
})

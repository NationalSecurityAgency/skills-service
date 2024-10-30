const { defineConfig } = require('cypress')
const fs = require('fs')
const { configureVisualRegression } = require('cypress-visual-regression')

module.exports = defineConfig({
  projectId: '7kivjf',
  requestTimeout: 10000,
  defaultCommandTimeout: 10000,
  numTestsKeptInMemory: 10,
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
  trashAssetsBeforeRuns: true,
  e2e: {
    // We've imported your old cypress plugins here.
    // You may want to clean this up later by importing these.
    setupNodeEvents(on, config) {
      on('after:spec', (spec, results) => {
        if (results && results.video) {
          // Do we have failures for any retry attempts?
          const failures = results.tests.some((test) =>
              test.attempts.some((attempt) => attempt.state === 'failed')
          )
          if (!failures) {
            // delete the video if the spec passed and no tests retried
            fs.unlinkSync(results.video)
          }
        }
      })

      configureVisualRegression(on);

      return require('./cypress/plugins/index.js')(on, config)
    },
    baseUrl: 'http://localhost:8080',
    specPattern: 'cypress/e2e/**/*.{js,jsx,ts,tsx}',
  },
  env: {
    "visualRegressionType": "regression",
    "visualRegressionBaseDirectory": "cypress/visualRegression/base",
    "visualRegressionDiffDirectory": "cypress/visualRegression/diff",
    "visualRegressionGenerateDiff": "fail"
  },
  reporter: 'mochawesome',
  reporterOptions: {
    reportDir: 'cypress/results',
    overwrite: false,
    html: false,
    json: true,
  },
})

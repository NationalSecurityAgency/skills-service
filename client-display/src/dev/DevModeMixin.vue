/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script>
  import TokenReauthorizer from '@/userSkills/service/TokenReauthorizer';

  export default {
    name: 'DevModeMixin',
    methods: {
      isDevelopmentMode() {
        return process.env.NODE_ENV === 'development';
      },
      configureDevelopmentMode() {
        if (!this.isValidDevelopmentMode()) {
          const errorMessage = `
            Development mode is not properly configured
            You must create a local file '.env.development.local' that defines:

            VUE_APP_AUTHENTICATION_URL
            VUE_APP_PROJECT_ID
            VUE_APP_SERVICE_URL

            For an example see .env.development.local.example
          `;

          // eslint-disable-next-line no-alert
          alert(errorMessage);
        } else {
          this.$store.commit('projectId', process.env.VUE_APP_PROJECT_ID);
          this.$store.commit('serviceUrl', process.env.VUE_APP_SERVICE_URL);
          if (process.env.VUE_APP_AUTHENTICATION_URL !== 'hydra') {
            let authenticator = process.env.VUE_APP_AUTHENTICATION_URL;
            if (this.$route.query.loginAsUser && this.$route.query.loginAsUser.length > 0) {
              authenticator = authenticator.replace('user0', this.$route.query.loginAsUser);
            }
            this.$store.commit('authenticator', authenticator);
          } else {
            this.$store.commit('authenticator', `${this.$store.state.serviceUrl}/api/projects/${this.$store.state.projectId}/token`);
            // eslint-disable-next-line
            console.log(`Authenticator set to hydra, setting new authenticator to [${this.$store.state.authenticator}]`);
          }
          this.storeAuthToken();

          const isSummaryOnly = this.$route.query.isSummaryOnly ? this.$route.query.isSummaryOnly : false;
          this.$store.commit('isSummaryOnly', isSummaryOnly);

          // whether or not to use an internal back button as opposed to the browser back button
          if (this.$route.query.internalBackButton == null) {
            // default to true
            this.$store.commit('internalBackButton', true);
          } else {
            this.$store.commit('internalBackButton', this.$route.query.internalBackButton);
          }

          const isThemeEnabled = this.$route.query.enableTheme ? this.$route.query.enableTheme : false;
          if (isThemeEnabled) {
            // eslint-disable-next-line global-require
            const theme = require('../../tests/data/theme.json');

            const themeParamProvided = this.$route.query.themeParam;
            if (themeParamProvided) {
              const split = themeParamProvided.split('|');
              const key = split[0];
              let val = split[1];
              if (val === 'null') {
                delete theme[key];
              } else {
                if (val.includes('{')) {
                  console.log(`parsing: ${val}`);
                  val = JSON.parse(val);
                }
                theme[key] = val;
              }
            }
            this.handleTheming(theme);
          }
        }
      },
      isValidDevelopmentMode() {
        return process.env.VUE_APP_AUTHENTICATION_URL && process.env.VUE_APP_PROJECT_ID && process.env.VUE_APP_SERVICE_URL;
      },
      storeAuthToken() {
        TokenReauthorizer.getAuthenticationToken()
          .then((result) => {
            this.$store.commit('authToken', result.data.access_token);
          });
      },
    },
  };
</script>

<style scoped>

</style>

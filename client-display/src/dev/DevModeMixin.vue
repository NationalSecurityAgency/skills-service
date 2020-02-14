<script>
  import SkillsConfiguration from '@skills/skills-client-configuration';
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
                SkillsConfiguration.configure({
                    serviceUrl: process.env.VUE_APP_SERVICE_URL,
                    projectId: process.env.VUE_APP_PROJECT_ID,
                    authenticator: process.env.VUE_APP_AUTHENTICATION_URL,
                });
                this.storeAuthToken();

                const isSummaryOnly = this.$route.query.isSummaryOnly ? this.$route.query.isSummaryOnly : false;
                this.$store.commit('isSummaryOnly', isSummaryOnly);

                const isThemeEnabled = this.$route.query.enableTheme ? this.$route.query.enableTheme : false;
                if (isThemeEnabled) {
                    // eslint-disable-next-line global-require
                    const theme = require('../../tests/data/theme.json');
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
                    SkillsConfiguration.setAuthToken(result.data.access_token);
                });
        },
    },
  };
</script>

<style scoped>

</style>

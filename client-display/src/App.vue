<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import store from '@/store';

  import Vue from 'vue';

  import Postmate from 'postmate';

  import debounce from 'lodash/debounce';
  import merge from 'lodash/merge';

  const getDocumentHeight = () => {
    const { body } = document;
    return Math.max(body.scrollHeight, body.offsetHeight);
  };

  const onHeightChanged = debounce(() => {
    if (process.env.NODE_ENV !== 'development') {
      store.state.parentFrame.emit('height-changed', getDocumentHeight());
    }
  }, 0);

  Vue.use({
    install() {
      Vue.mixin({
        updated() {
          onHeightChanged();
        },
      });
    },
  });

  export default {
    mounted() {
      // console.log('remove this line');
      // this.handleTheming();

      const vm = this;
      if (this.isDevelopmentMode()) {
        this.configureDevelopmentMode();
      } else {
        const handshake = new Postmate.Model({
          updateAuthenticationToken(authToken) {
            store.commit('authToken', authToken);
            UserSkillsService.setToken(authToken);
          },
          updateVersion(newVersion) {
            UserSkillsService.setVersion(newVersion);
            vm.$router.push({
              name: 'home',
            });
          },
        });


        handshake.then((parent) => {
          // Make sure to freeze the parent object so Vuex won't try to make it reactive
          // CORs won't allow this because parent object can't be changed from an iframe
          this.$store.commit('parentFrame', Object.freeze(parent));
          window.addEventListener('resize', onHeightChanged);
          this.onHeightChange();

          UserSkillsService.setServiceUrl(parent.model.serviceUrl);
          UserSkillsService.setProjectId(parent.model.projectId);
          UserSkillsService.setVersion(parent.model.version);
          UserSkillsService.setUserId(parent.model.userId);

          this.handleTheming(parent.model.theme);

          this.$store.state.parentFrame.emit('needs-authentication');

          // No scroll bars for iframe.
          document.body.style['overflow-y'] = 'hidden';
        });
      }
    },
    methods: {
      handleTheming() {
        const nonCSSConfig = ['progressIndicators'];

        const selectorKey = {
          backgroundColor: {
            selector: 'body #app',
            styleName: 'background-color',
          },
          secondaryTextColor: {
            selector: 'body #app .text-muted, body #app .text-secondary',
            styleName: 'color',
          },
          primaryTextColor: {
            selector: 'body #app .card-header, body #app .card-body, body #app .skill-tile-label',
            styleName: 'color',
          },
          tiles: {
            backgroundColor: {
              selector: 'body #app .card-header, body #app .card-body, body #app .card-footer',
              styleName: 'background-color',
            },
            watermarkIconColor: {
              selector: 'body #app .card-body .watermark-icon',
              styleName: 'color',
            },
          },
          stars: {
            unearnedColor: {
              selector: 'body #app .star-empty',
              styleName: 'color',
            },
            earnedColor: {
              selector: 'body #app .star-filled',
              styleName: 'color',
            },
          },
        };

        const mockTheme = {
          // backgroundColor: {
          //   value: 'grey',
          // },
          // secondaryTextColor: {
          //   value: 'purple',
          // },
          // primaryTextColor: {
          //   value: 'green',
          // },
          // stars: {
          //   unearnedColor: {
          //     value: 'blue'
          //   },
          //   earnedColor: {
          //     value: 'green',
          //   }
          // },
          // progressIndicators: {
          //   beforeTodayColor: '#3e4d44',
          //   earnedTodayColor: '#667da4',
          //   completeColor: '#59ad52',
          //   incompleteColor: '#cdcdcd',
          // },
          // tiles: {
          //   backgroundColor: {
          //     value: '#D6EAF8',
          //   },
          //   watermarkIconColor: {
          //     value: 'orange',
          //   },
          // },
        };

        const themeKey = merge(JSON.parse(JSON.stringify(selectorKey)), mockTheme);

        const { body } = document;

        let css = '';
        const buildCss = (obj, keys) => {
          keys.forEach((key) => {
            const isCSSConfig = !nonCSSConfig.includes(key);
            if (isCSSConfig && (obj[key].value || obj[key].selector || obj[key].styleName)) {
              const { selector, styleName, value } = obj[key];
              if (!selector || !styleName) {
                throw new Error(`Invalid custom theme defined by ${key}`);
              } else if (value) {
                const sanitizedValue = value.split(';')[0]; // No injection
                css += `${selector} { ${styleName}: ${sanitizedValue} !important }`;
              }
            } else if (isCSSConfig) {
              buildCss(themeKey[key], Object.keys(themeKey[key]));
            }
          });
        };

        buildCss(themeKey, Object.keys(themeKey));

        const style = document.createElement('style');
        style.appendChild(document.createTextNode(css));

        body.appendChild(style);
      },

      onHeightChange() {
        onHeightChanged();
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
          UserSkillsService.setAuthenticationUrl(process.env.VUE_APP_AUTHENTICATION_URL);
          UserSkillsService.setServiceUrl(process.env.VUE_APP_SERVICE_URL);
          UserSkillsService.setProjectId(process.env.VUE_APP_PROJECT_ID);
          this.storeAuthToken();
        }
      },

      isDevelopmentMode() {
        return process.env.NODE_ENV === 'development';
      },

      isValidDevelopmentMode() {
        return process.env.VUE_APP_AUTHENTICATION_URL && process.env.VUE_APP_PROJECT_ID && process.env.VUE_APP_SERVICE_URL;
      },

      storeAuthToken() {
        UserSkillsService.getAuthenticationToken()
          .then((result) => {
            this.$store.commit('authToken', result);
            UserSkillsService.setToken(result.access_token);
          });
      },
    },
  };
</script>

<style>
  @import '../node_modules/animate.css/animate.min.css';
  @import '../node_modules/material-icons/iconfont/material-icons.css';
  @import '../node_modules/material-icons/css/material-icons.css';
  @import '../node_modules/@fortawesome/fontawesome-free/css/all.css';

  #app {
    max-width: 1100px;
    margin: 0 auto;
    text-align: center;
    overflow: hidden;
  }
</style>

<style lang="scss">
  @import "./assets/_common.scss";
  @import "~bootstrap/scss/bootstrap";
</style>

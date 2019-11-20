<template>
  <div
    id="app"
    :style="appStyleObject">
    <router-view />
  </div>
</template>

<script>
  import Vue from 'vue';

  import Postmate from 'postmate';

  import debounce from 'lodash/debounce';
  import merge from 'lodash/merge';

  import SkillsConfiguration from '@skills/skills-client-configuration';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import TokenReauthorizer from '@/userSkills/service/TokenReauthorizer';
  import store from '@/store';

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
    data() {
      return {
        appStyleObject: {},
      };
    },
    mounted() {
      // this.handleTheming();

      const vm = this;
      if (this.isDevelopmentMode()) {
        this.configureDevelopmentMode();
        this.$store.commit('isSummaryOnly', false);
      } else {
        const handshake = new Postmate.Model({
          updateAuthenticationToken(authToken) {
            store.commit('authToken', authToken);
            SkillsConfiguration.setAuthToken(authToken);
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

          // will only display summary and component will not be interactive
          this.$store.commit('isSummaryOnly', parent.model.isSummaryOnly ? parent.model.isSummaryOnly : false);

          UserSkillsService.setVersion(parent.model.version);
          UserSkillsService.setUserId(parent.model.userId);

          SkillsConfiguration.configure({
            projectId: parent.model.projectId,
            serviceUrl: parent.model.serviceUrl,
          });

          this.handleTheming(parent.model.theme);

          this.$store.state.parentFrame.emit('needs-authentication');

          if (parent.model.minHeight) {
            Vue.set(this.appStyleObject, 'min-height', parent.model.minHeight);
          }

          // No scroll bars for iframe.
          document.body.style['overflow-y'] = 'hidden';
        });
      }
    },
    methods: {
      handleTheming(theme) {
        if (theme) {
          const nonCSSConfig = ['progressIndicators', 'charts'];

          const selectorKey = {
            backgroundColor: {
              selector: 'body #app',
              styleName: 'background-color',
            },
            trophyIconColor: {
              selector: 'body #app .fa.fa-trophy',
              styleName: 'color',
            },
            subjectTileIconColor: {
              selector: 'body #app .subject-tile-icon',
              styleName: 'color',
            },
            pageTitleTextColor: {
              selector: 'body #app .skills-page-title-text-color',
              styleName: 'color',
            },
            circleProgressInteriorTextColor: {
              selector: 'body #app .circle-number span',
              styleName: 'color',
            },
            textPrimaryColor: {
              selector: 'body #app .text-primary, body #app',
              styleName: 'color',
            },
            textPrimaryMutedColor: {
              selector: 'body #app .text-primary .text-muted, body #app .text-primary.text-muted',
              styleName: 'color',
            },
            textSecondaryColor: {
              selector: 'body #app .text-muted, body #app .text-secondary',
              styleName: 'color',
            },
            tiles: {
              backgroundColor: {
                selector: 'body #app .card, body #app .card-header, body #app .card-body, body #app .card-footer',
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
            graphLegendBorderColor: {
              selector: 'body #app .graph-legend .card-header, body #app .graph-legend .card-body',
              styleName: 'border',
            },
          };

          const { body } = document;

          const expandValue = (object, key) => {
            const isCSSConfig = !nonCSSConfig.includes(key);
            if (isCSSConfig && typeof object[key] === 'object') {
              Object.keys(object[key]).forEach((childKey) => {
                expandValue(object[key], childKey);
              });
            } else if (isCSSConfig) {
              // eslint-disable-next-line no-param-reassign
              object[key] = { value: object[key] };
            }
          };

          Object.keys(theme).forEach((key) => {
            expandValue(theme, key);
          });

          const themeKey = merge(JSON.parse(JSON.stringify(selectorKey)), theme);

          let css = '';
          const buildCss = (obj, keys) => {
            keys.forEach((key) => {
              const isCSSConfig = !nonCSSConfig.includes(key);
              if (isCSSConfig && (obj[key].value || obj[key].selector || obj[key].styleName)) {
                const { selector, styleName, value } = obj[key];
                if (!selector || !styleName) {
                  throw new Error(`Skills Theme Error! Failed to process provided custom theme due to invalid format! Invalid custom theme defined by [${key}]. Theme is ${JSON.stringify(theme)}`);
                } else if (value) {
                  const sanitizedValue = value.split(';')[0]; // No injection
                  css += `${selector} { ${styleName}: ${sanitizedValue} !important }`;
                }
              } else if (isCSSConfig) {
                buildCss(themeKey[key], Object.keys(themeKey[key]));
              } else {
                this.$store.state.themeModule[key] = { ...this.$store.state.themeModule[key], ...themeKey[key] };
              }
            });
          };

          buildCss(themeKey, Object.keys(themeKey));

          // Some CSS may mess up some things, fix those here
          // Apex charts context menu
          css += 'body #app .apexcharts-menu.open { color: black !important; }';
          css += 'body #app .apexcharts-tooltip { color: black !important; }';

          const style = document.createElement('style');

          style.id = this.$store.state.themeStyleId;
          style.appendChild(document.createTextNode(css));

          body.appendChild(style);
        }
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
          SkillsConfiguration.configure({
            serviceUrl: process.env.VUE_APP_SERVICE_URL,
            projectId: process.env.VUE_APP_PROJECT_ID,
            authenticator: process.env.VUE_APP_AUTHENTICATION_URL,
          });
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
        TokenReauthorizer.getAuthenticationToken()
          .then((result) => {
            this.$store.commit('authToken', result.data.access_token);
            SkillsConfiguration.setAuthToken(result.data.access_token);
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
    /*max-width: 1100px;*/
    margin: 0 auto;
    text-align: center;
    overflow: hidden;
  }

  #app .text-primary {
    color: #4f565d !important;
  }

  .card {
    /*box-shadow: 0 0.75rem 1.5rem rgba(18,38,63,.5);*/
  }
</style>

<style lang="scss">
  @import "./assets/_common.scss";
  @import "~bootstrap/scss/bootstrap";
</style>

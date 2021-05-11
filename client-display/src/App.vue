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
<template>
  <div
    id="app"
    class="container-fluid skills-display-container py-2"
    :style="appStyleObject" role="main" aria-label="SkillTree Client Display">
    <new-software-version-component/>
    <router-view />
  </div>
</template>

<script>
  import Vue from 'vue';

  import Postmate from 'postmate';

  import debounce from 'lodash/debounce';

  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import store from '@/store';
  import NewSoftwareVersionComponent from '@/common/softwareVersion/NewSoftwareVersion';
  import DevModeMixin from '@/dev/DevModeMixin';
  import ThemeHelper from './common/theme/ThemeHelper';

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
    mixins: [DevModeMixin],
    components: { NewSoftwareVersionComponent },
    data() {
      return {
        appStyleObject: {},
      };
    },
    created() {
      const vm = this;
      if (this.isDevelopmentMode()) {
        this.configureDevelopmentMode();
      } else {
        const handshake = new Postmate.Model({
          updateAuthenticationToken(authToken) {
            store.commit('authToken', authToken);
          },
          updateVersion(newVersion) {
            UserSkillsService.setVersion(newVersion);
            vm.$router.push({
              name: 'home',
            });
          },
          navigate(fullPath) {
            vm.$router.replace(fullPath);
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

          // whether or not to use an internal back button as opposed to the browser back button
          this.$store.commit('internalBackButton', parent.model.internalBackButton ? parent.model.internalBackButton : true);

          this.$store.commit('projectId', parent.model.projectId);
          this.$store.commit('serviceUrl', parent.model.serviceUrl);

          UserSkillsService.setVersion(parent.model.version);
          UserSkillsService.setUserId(parent.model.userId);

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
          const themeResArtifacts = ThemeHelper.build(theme);

          // populate store so JS can subscribe to those values and update styles
          const self = this;
          themeResArtifacts.themeModule.forEach((value, key) => {
            if (typeof value === 'object') {
              self.$store.state.themeModule[key] = { ...self.$store.state.themeModule[key], ...value };
            } else {
              self.$store.state.themeModule[key] = value;
            }
          });

          const style = document.createElement('style');

          style.id = this.$store.state.themeStyleId;
          style.appendChild(document.createTextNode(themeResArtifacts.css));

          const { body } = document;
          body.appendChild(style);
        }
      },

      onHeightChange() {
        onHeightChanged();
      },
    },
  };
</script>

<style>
  @import '../node_modules/animate.css/animate.min.css';
  @import '../node_modules/material-icons/iconfont/material-icons.css';
  @import '../node_modules/material-icons/css/material-icons.css';
  @import '../node_modules/@fortawesome/fontawesome-free/css/all.css';

  .skills-display-container {
    max-width: 1140px;
  }

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
  @import "./assets/custom.scss";
</style>

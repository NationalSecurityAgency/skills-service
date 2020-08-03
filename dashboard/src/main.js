/*
 * Copyright 2020 SkillTree
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
// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue';
import BootstrapVue from 'bootstrap-vue';
import { ClientTable, ServerTable } from 'vue-tables-2';
import { SkillsConfiguration, SkillsDirective, SkillsReporter } from '@skilltree/skills-client-vue';
import VeeValidate from 'vee-validate';
import VueApexCharts from 'vue-apexcharts';
import Vuex from 'vuex';
import VueDraggableResizable from 'vue-draggable-resizable';
import InceptionConfigurer from './InceptionConfigurer';
import 'babel-polyfill';
import 'matchmedia-polyfill';
import 'matchmedia-polyfill/matchMedia.addListener';
import './filters/NumberFilter';
import './filters/TruncateFilter';
import './filters/DateFilter';
import './directives/SkillsOnMountDirective';
import RegisterValidators from './validators/RegisterValidators';
import './directives/FocusDirective';
import App from './App';
import router from './router';
import store from './store/store';

Vue.use(ClientTable, {}, false, 'bootstrap4', 'default');
Vue.use(ServerTable, {}, false, 'bootstrap4', 'default');
Vue.use(VeeValidate);
Vue.use(Vuex);
Vue.use(VueApexCharts);
Vue.use(BootstrapVue);
Vue.use(SkillsDirective);
Vue.use('vue-draggable-resizable', VueDraggableResizable);

VeeValidate.setMode('betterEager', () => ({ on: ['input'], debounce: 500 }));

Vue.component('apexchart', VueApexCharts);

Vue.config.productionTip = false;

window.moment = require('moment');
window.axios = require('axios');

require('./interceptors/errorHandler');
require('./interceptors/clientVersionInterceptor');

require('vue-multiselect/dist/vue-multiselect.min.css');

const isActiveProjectIdChange = (to, from) => to.params.projectId !== from.params.projectId;
const isLoggedIn = () => store.getters.isAuthenticated;
const isPki = () => store.getters.isPkiAuthenticated;

router.beforeEach((to, from, next) => {
  const requestAccountPath = '/request-root-account';
  if (!isPki() && !isLoggedIn() && to.path !== requestAccountPath && store.getters.config.needToBootstrap) {
    next({ path: requestAccountPath });
  } else if (!isPki() && to.path === requestAccountPath && !store.getters.config.needToBootstrap) {
    next({ path: '/' });
  } else {
    if (from.path !== '/error') {
      store.commit('previousUrl', from.fullPath);
    }
    if (isActiveProjectIdChange(to, from)) {
      store.commit('currentProjectId', to.params.projectId);
    }
    if (to.matched.some(record => record.meta.requiresAuth)) {
      // this route requires auth, check if logged in if not, redirect to login page.
      if (!isLoggedIn()) {
        const newRoute = { query: { redirect: to.fullPath } };
        if (isPki()) {
          newRoute.name = 'HomePage';
        } else {
          newRoute.name = 'Login';
        }
        next(newRoute);
      } else {
        next();
      }
    } else {
      next();
    }
  }
});

router.afterEach((to) => {
  if (to.meta.reportSkillId) {
    SkillsConfiguration.afterConfigure()
      .then(() => {
        SkillsReporter.reportSkill(to.meta.reportSkillId);
      });
  }
});

store.dispatch('loadConfigState').finally(() => {
  RegisterValidators.init();
  store.dispatch('restoreSessionIfAvailable').finally(() => {
    InceptionConfigurer.configure();
    /* eslint-disable no-new */
    const vm = new Vue({
      el: '#app',
      router,
      components: { App },
      template: '<App/>',
      store,
    });
    window.vm = vm;
  });
});

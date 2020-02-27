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
import { SkillsDirective } from '@skills/skills-client-vue';
import VeeValidate from 'vee-validate';
import VueApexCharts from 'vue-apexcharts';
import Vuex from 'vuex';
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

VeeValidate.setMode('betterEager', () => ({ on: ['input'], debounce: 500 }));

Vue.component('apexchart', VueApexCharts);

Vue.config.productionTip = false;

window.moment = require('moment');
window.axios = require('axios');

require('./interceptors/errorHandler');
require('./interceptors/clientVersionInterceptor');

require('vue-multiselect/dist/vue-multiselect.min.css');

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

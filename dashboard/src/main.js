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
import {
  ButtonPlugin,
  ToastPlugin,
  ButtonGroupPlugin,
  TooltipPlugin,
  ModalPlugin,
  LayoutPlugin,
  FormRadioPlugin,
  AlertPlugin,
  FormSelectPlugin,
  SpinnerPlugin,
  TabsPlugin,
  FormTextareaPlugin,
  LinkPlugin,
  DropdownPlugin,
  AvatarPlugin,
  TablePlugin,
  FormInputPlugin,
  FormCheckboxPlugin,
  InputGroupPlugin,
  CardPlugin,
  PaginationPlugin,
  CollapsePlugin,
  OverlayPlugin,
  BadgePlugin,
  PopoverPlugin,
  FormPlugin,
  FormGroupPlugin,
  FormDatepickerPlugin,
  ProgressPlugin,
  BIcon,
  BIconQuestion,
} from 'bootstrap-vue';

import { SkillsConfiguration, SkillsDirective, SkillsReporter } from '@skilltree/skills-client-vue';
import {
  localize, ValidationProvider, ValidationObserver, setInteractionMode,
} from 'vee-validate';
import en from 'vee-validate/dist/locale/en.json';
import Vuex from 'vuex';
import InceptionConfigurer from './InceptionConfigurer';
import 'babel-polyfill';
import 'matchmedia-polyfill';
import 'matchmedia-polyfill/matchMedia.addListener';
import dayjs from './DayJsCustomizer';
import './filters/NumberFilter';
import './filters/TruncateFilter';
import './filters/DateFilter';
import './filters/TimeFromNowFilter';
import './directives/SkillsOnMountDirective';
import RegisterValidators from './validators/RegisterValidators';
import './directives/FocusDirective';
import App from './App';
import router from './router';
import store from './store/store';

const getApex = () => import(
  /* webpackChunkName: "apexCharts" */
  'vue-apexcharts'
);

Vue.component('ValidationProvider', ValidationProvider);
Vue.component('ValidationObserver', ValidationObserver);
Vue.use(Vuex);

Vue.use(ButtonPlugin);
Vue.use(ToastPlugin);
Vue.use(TooltipPlugin);
Vue.use(LayoutPlugin);
Vue.use(FormRadioPlugin);
Vue.use(AlertPlugin);
Vue.use(FormSelectPlugin);
Vue.use(ModalPlugin);
Vue.use(SpinnerPlugin);
Vue.use(TabsPlugin);
Vue.use(FormTextareaPlugin);
Vue.use(LinkPlugin);
Vue.use(DropdownPlugin);
Vue.use(AvatarPlugin);
Vue.use(ButtonGroupPlugin);
Vue.use(TablePlugin);
Vue.use(FormInputPlugin);
Vue.use(InputGroupPlugin);
Vue.use(FormCheckboxPlugin);
Vue.use(CardPlugin);
Vue.use(PaginationPlugin);
Vue.use(CollapsePlugin);
Vue.use(OverlayPlugin);
Vue.use(BadgePlugin);
Vue.use(PopoverPlugin);
Vue.use(FormPlugin);
Vue.use(FormGroupPlugin);
Vue.use(FormDatepickerPlugin);
Vue.use(ProgressPlugin);
Vue.component('BIcon', BIcon);
Vue.component('BIconQuestion', BIconQuestion);

Vue.use(SkillsDirective);

localize({
  en,
});

setInteractionMode('custom', () => ({ on: ['input', 'change'] }));
Vue.config.productionTip = false;
window.dayjs = dayjs;

window.axios = require('axios');
require('./interceptors/errorHandler');
require('./interceptors/clientVersionInterceptor');
require('vue-multiselect/dist/vue-multiselect.min.css');

const isActiveProjectIdChange = (to, from) => to.params.projectId !== from.params.projectId;
const isLoggedIn = () => store.getters.isAuthenticated;
const isPki = () => store.getters.isPkiAuthenticated;
const getLandingPage = () => {
  let landingPage = 'MyProgressPage';
  if (store.getters.userInfo) {
    if (store.getters.userInfo.landingPage === 'admin') {
      landingPage = 'AdminHomePage';
    }
  }
  return landingPage;
};

router.beforeEach((to, from, next) => {
  const requestAccountPath = '/request-root-account';
  if (!isPki() && !isLoggedIn() && to.path !== requestAccountPath && store.getters.config.needToBootstrap) {
    next({ path: requestAccountPath });
  } else if (!isPki() && to.path === requestAccountPath && !store.getters.config.needToBootstrap) {
    next({ name: getLandingPage() });
  } else {
    if (to.path === '/') {
      const landingPageRoute = { name: getLandingPage() };
      next(landingPageRoute);
    }
    if (from.path !== '/error') {
      store.commit('previousUrl', from.fullPath);
    }
    if (isActiveProjectIdChange(to, from)) {
      store.commit('currentProjectId', to.params.projectId);
    }
    if (to.matched.some((record) => record.meta.requiresAuth)) {
      // this route requires auth, check if logged in if not, redirect to login page.
      if (!isLoggedIn()) {
        const newRoute = { query: { redirect: to.fullPath } };
        if (isPki()) {
          newRoute.name = getLandingPage();
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
    getApex().then((VueApexCharts) => {
      Vue.component('apexchart', VueApexCharts.default);
      Vue.use(VueApexCharts.default);
    });
  });
});

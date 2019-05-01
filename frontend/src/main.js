// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue';
import BootstrapVue from 'bootstrap-vue';
import { ClientTable, ServerTable } from 'vue-tables-2';
import Vue2Crumbs from 'vue-2-crumbs';
import VeeValidate from 'vee-validate';
import VueApexCharts from 'vue-apexcharts';
import Vuex from 'vuex';
import 'babel-polyfill';
import 'matchmedia-polyfill';
import 'matchmedia-polyfill/matchMedia.addListener';
import './filters/NumberFilter';
import './filters/TruncateFilter';
import App from './App';
import router from './router';
import store from './store/store';

Vue.use(ClientTable, {}, false, 'bootstrap4', 'default');
Vue.use(ServerTable, {}, false, 'bootstrap4', 'default');
Vue.use(Vue2Crumbs);
Vue.use(VeeValidate);
Vue.use(Vuex);
Vue.use(VueApexCharts);
Vue.use(BootstrapVue);

Vue.component('apexchart', VueApexCharts);

Vue.config.productionTip = false;

window.moment = require('moment');
window.axios = require('axios');

require('./errorHandler');

Vue.directive('focus', {
  inserted: (e1) => {
    e1.focus();
  },
});

require('vue-multiselect/dist/vue-multiselect.min.css');

store.dispatch('restoreSessionIfAvailable').then(() => {
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

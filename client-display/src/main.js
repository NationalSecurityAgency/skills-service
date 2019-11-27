import VueApexCharts from 'vue-apexcharts';

import Vue from 'vue';
import App from '@/App.vue';

import router from '@/router';
import store from '@/store';

import 'apexcharts';

Vue.config.productionTip = false;

Vue.use(VueApexCharts);
Vue.use(require('vue-moment'));

require('@/common/softwareVersion/softwareVersionInterceptor');

new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app');

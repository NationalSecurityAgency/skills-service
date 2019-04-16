import Vue from 'vue';
import App from '@/App.vue';

import Router from '@/router';

import 'apexcharts';
import VueApexCharts from 'vue-apexcharts';

Vue.config.productionTip = false;

Vue.use(VueApexCharts);
Vue.use(require('vue-moment'));

new Vue({
  router: Router,
  render: h => h(App),
}).$mount('#app');

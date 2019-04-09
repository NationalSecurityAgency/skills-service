import Vue from 'vue';
import App from '@/App.vue';

import Router from '@/router';

import 'apexcharts';
import VueApexCharts from 'vue-apexcharts';

Vue.config.productionTip = false;

Vue.use(VueApexCharts);

new Vue({
  router: Router,
  render: h => h(App),
}).$mount('#app');

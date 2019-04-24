import Vue from 'vue';
import App from '@/App.vue';

import router from '@/router';
import store from '@/store';

import 'apexcharts';
import VueApexCharts from 'vue-apexcharts';

Vue.config.productionTip = false;

Vue.use(VueApexCharts);
Vue.use(require('vue-moment'));

new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app');

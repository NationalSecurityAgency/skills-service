import Vue from 'vue';
import VeeValidate from 'vee-validate';
import App from './App';

Vue.use(VeeValidate);
Vue.config.productionTip = false;

new Vue({ // eslint-disable-line no-new
  el: '#app',
  components: { App },
  template: '<App/>',
});

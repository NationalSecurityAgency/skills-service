import Vue from 'vue';

Vue.directive('focus', {
  inserted: (el) => {
    setTimeout(() => el.focus(), 10);
  },
});

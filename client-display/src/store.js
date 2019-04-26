import Vue from 'vue';
import Vuex from 'vuex';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    authToken: null,
  },
  mutations: {
    authToken(state, authToken) {
      // eslint-disable-next-line no-param-reassign
      state.authToken = authToken;
    },
  },
});

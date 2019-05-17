import Vue from 'vue';
import Vuex from 'vuex';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    authToken: null,
    isAuthenticating: false,
    parentFrame: null,
  },
  mutations: {
    authToken(state, authToken) {
      // eslint-disable-next-line no-param-reassign
      state.authToken = authToken;
    },
    parentFrame(state, parentFrame) {
      // eslint-disable-next-line no-param-reassign
      state.parentFrame = parentFrame;
    },
    isAuthenticating(state, isAuthenticating) {
      // eslint-disable-next-line no-param-reassign
      state.isAuthenticating = isAuthenticating;
    },
  },
});

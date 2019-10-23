import Vue from 'vue';
import Vuex from 'vuex';
import axios from 'axios';
import UniqueIdGenerator from '@/common/utilities/UniqueIdGenerator';

Vue.use(Vuex);

const themeModule = {
  state: {
    progressIndicators: {
      beforeTodayColor: '#14a3d2',
      earnedTodayColor: '#7ed6f3',
      completeColor: '#59ad52',
      incompleteColor: '#cdcdcd',
    },
    charts: {
      axisLabelColor: 'black',
    },
  },
};

export default new Vuex.Store({
  modules: {
    themeModule,
  },
  state: {
    authToken: null,
    isAuthenticating: false,
    parentFrame: null,
    version: null,
    themeStyleId: UniqueIdGenerator.uniqueId('custom-theme-style-node-'),
    isSummaryOnly: false,
  },
  mutations: {
    authToken(state, authToken) {
      axios.defaults.headers.common.Authorization = `Bearer ${authToken}`;
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
    version(state, version) {
      // eslint-disable-next-line no-param-reassign
      state.version = version;
    },
    isSummaryOnly(state, isSummaryOnly) {
      // eslint-disable-next-line no-param-reassign
      state.isSummaryOnly = isSummaryOnly;
    },
  },
});

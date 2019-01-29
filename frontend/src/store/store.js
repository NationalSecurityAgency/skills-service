import Vue from 'vue';
import Vuex from 'vuex';
import auth from './modules/auth';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    projectId: '',
  },
  mutations: {
    currentProjectId(state, projectId) {
      state.projectId = projectId;
    },
  },
  modules: {
    auth,
  },
});

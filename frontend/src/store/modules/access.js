import AccessService from '../../components/access/AccessService';

const actions = {
  isSupervisor({ commit }) {
    return new Promise((resolve, reject) => {
      console.log('store.access.actions.isSupervisor, calling AccessService');
      AccessService.hasRole('ROLE_SUPERVISOR').then((result) => {
        console.log(`store.access.actions.isSupervisor - hasRole ROLE_SUPERVISOR: ${result}`);
        commit('supervisor', result);
        resolve(result);
      }).catch(error => reject(error));
    });
  },
};

const mutations = {
  supervisor(state, value) {
    console.log(`store.access.setSupervisor to ${value}`);
    state.isSupervisor = value;
  },
};

const getters = {
  isSupervisor(state) {
    return state.isSupervisor;
  },
};

const state = {
  isSupervisor: null,
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
};

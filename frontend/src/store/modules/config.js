import SettingsService from '../../components/settings/SettingsService';

const getters = {
  config(state) {
    return state.config;
  },
};

const mutations = {
  setConfig(state, value) {
    state.config = value;
  },
};

const actions = {
  loadConfigState({ commit }) {
    return new Promise((resolve, reject) => {
      SettingsService.getConfig()
        .then((response) => {
          commit('setConfig', response);
          resolve(response);
        })
        .catch(error => reject(error));
    });
  },
};

const state = {
  config: null,
};

export default {
  namespaced: false,
  state,
  getters,
  mutations,
  actions,
};

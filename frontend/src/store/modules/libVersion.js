const getters = {
  libVersion(state) {
    return state.libVersion;
  },
};

const mutations = {
  setLibVersion(state, value) {
    state.libVersion = value;
  },
};

const actions = {
  updateLibVersionIfDifferent({ commit, state }, incomingVersion) {
    if (state.libVersion === undefined || state.libVersion !== incomingVersion) {
      commit('setLibVersion', incomingVersion);
    }
  },
};

const state = {
  libVersion: undefined,
};

export default {
  namespaced: false,
  state,
  getters,
  mutations,
  actions,
};

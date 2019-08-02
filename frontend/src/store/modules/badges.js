import BadgesService from '../../components/badges/BadgesService';

const getters = {
  badge(state) {
    return state.badge;
  },
};

const mutations = {
  setBadge(state, value) {
    state.badge = value;
  },
};

const actions = {
  loadBadgeDetailsState({ commit }, payload) {
    return new Promise((resolve, reject) => {
      BadgesService.getBadge(payload.projectId, payload.badgeId)
        .then((response) => {
          commit('setBadge', response);
          resolve(response);
        })
        .catch(error => reject(error));
    });
  },
};

const state = {
  badge: null,
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
};

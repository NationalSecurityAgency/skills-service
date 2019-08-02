import UsersService from '../../components/users/UsersService';

const getters = {
  numSkills(state) {
    return state.numSkills;
  },
  userTotalPoints(state) {
    return state.userTotalPoints;
  },
};

const mutations = {
  setNumSkills(state, value) {
    state.numSkills = value;
  },
  setUserTotalPoints(state, value) {
    state.userTotalPoints = value;
  },
  incrementNumSkills(state, value) {
    state.numSkills += value;
  },
  decrementNumSkills(state, value) {
    state.numSkills -= value;
  },
  incrementUserTotalPoints(state, value) {
    state.userTotalPoints += value;
  },
  decrementUserTotalPoints(state, value) {
    state.userTotalPoints -= value;
  },
};

const actions = {
  loadUserDetailsState({ commit }, payload) {
    return new Promise((resolve, reject) => {
      UsersService.getUserSkillsMetrics(payload.projectId, payload.userId)
        .then((response) => {
          commit('setNumSkills', response.numSkills);
          commit('setUserTotalPoints', response.userTotalPoints);
          resolve(response);
        })
        .catch(error => reject(error));
    });
  },
};

const state = {
  numSkills: 0,
  userTotalPoints: 0,
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
};

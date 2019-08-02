import ProjectsService from '../../components/projects/ProjectService';

const getters = {
  project(state) {
    return state.project;
  },
};

const mutations = {
  setProject(state, value) {
    state.project = value;
  },
};

const actions = {
  loadProjectDetailsState({ commit }, payload) {
    return new Promise((resolve, reject) => {
      ProjectsService.getProjectDetails(payload.projectId)
        .then((response) => {
          commit('setProject', response);
          resolve(response);
        })
        .catch(error => reject(error));
    });
  },
};

const state = {
  project: null,
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
};

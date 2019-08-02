import SubjectsService from '../../components/subjects/SubjectsService';

const getters = {
  subject(state) {
    return state.subject;
  },
};

const mutations = {
  setSubject(state, value) {
    state.subject = value;
  },
};

const actions = {
  loadSubjectDetailsState({ commit }, payload) {
    return new Promise((resolve, reject) => {
      SubjectsService.getSubjectDetails(payload.projectId, payload.subjectId)
        .then((response) => {
          commit('setSubject', response);
          resolve(response);
        })
        .catch(error => reject(error));
    });
  },
};

const state = {
  subject: null,
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
};

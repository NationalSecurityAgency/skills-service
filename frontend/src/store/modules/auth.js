import axios from 'axios';
import router from '../../router';

const getters = {
  userInfo(state) {
    return state.userInfo;
  },
  isAuthenticated(state) {
    return (state.token !== null || state.pkiAuth) && state.userInfo;
  },
  isPkiAuthenticated(state) {
    return state.pkiAuth;
  },
};

const mutations = {
  authUser(state, authData) {
    state.token = authData.token;
    localStorage.setItem('token', authData.token);
    localStorage.setItem('expirationDate', authData.expirationDate.getTime().toString());
    axios.defaults.headers.common.Authorization = authData.token;
  },
  storeUser(state, userInfo) {
    state.userInfo = userInfo;
  },
  clearAuthData(state) {
    state.token = null;
    state.userId = null;
    localStorage.removeItem('token');
    localStorage.removeItem('expirationDate');
    localStorage.removeItem('userInfo');
    delete axios.defaults.headers.common.Authorization;
  },
};

const actions = {
  signup({ commit, dispatch }, authData) {
    return new Promise((resolve, reject) => {
      axios.put('/createAccount', authData)
        .then((result) => {
          const token = result.headers.authorization;
          const expirationDate = new Date(Number(result.headers.tokenexpirationtimestamp));
          commit('authUser', { token, expirationDate });
          dispatch('fetchUser')
            .then(() => {
              resolve(result);
          });
          dispatch('setLogoutTimer', expirationDate);
        })
        .catch(error => reject(error));
    });
  },
  login({ commit, dispatch }, authData) {
    return new Promise((resolve, reject) => {
      axios.post('/performLogin', authData)
        .then((result) => {
          const token = result.headers.authorization;
          const expirationDate = new Date(Number(result.headers.tokenexpirationtimestamp));
          commit('authUser', { token, expirationDate });
          dispatch('fetchUser')
            .then(() => {
              resolve(result);
          });
          dispatch('setLogoutTimer', expirationDate);
        })
        .catch(error => reject(error));
    });
  },
  restoreSessionIfAvailable({ commit, dispatch, state }) {
    return new Promise((resolve, reject) => {
      let reAuthenticated = false;
      const token = localStorage.getItem('token');
      if (token) {
        let tokenExpired = true;
        let expirationDate = localStorage.getItem('expirationDate');
        if (expirationDate) {
          expirationDate = new Date(Number(expirationDate));
          if (expirationDate > new Date()) {
            tokenExpired = false;
          }
        }
        if (tokenExpired) {
          commit('clearAuthData');
        } else {
          // found valid token, update authentication properties
          const userInfo = JSON.parse(localStorage.getItem('userInfo'));
          commit('storeUser', userInfo);
          commit('authUser', { token, expirationDate });
          dispatch('setLogoutTimer', expirationDate);
          reAuthenticated = true;
        }
        resolve(reAuthenticated);
      } else {
        // attempt to retrieve userInfo using PKI (2-way ssl)
        dispatch('fetchUser', false).then(() => {
          if (state.userInfo) {
            state.pkiAuth = true;
            reAuthenticated = true;
          }
          resolve(reAuthenticated);
        }).catch(error => reject(error));
      }
    });
  },
  logout({ commit }) {
    commit('clearAuthData');
    router.replace('/');
  },
  setLogoutTimer({ dispatch }, expirationDate) {
    const expiresInMillis = expirationDate.getTime() - new Date().getTime();
    setTimeout(() => { dispatch('logout'); }, expiresInMillis);
  },
  fetchUser({ commit }, storeInLocalStorage = true) {
    return new Promise((resolve, reject) => {
      axios.get('/app/userInfo')
        .then((response) => {
          commit('storeUser', response.data);
          if (storeInLocalStorage) {
            localStorage.setItem('userInfo', JSON.stringify(response.data));
          }
          resolve(response);
        })
        .catch(error => reject(error));
    });
  },
};

const state = {
  token: null,
  userInfo: null,
  pkiAuth: false,
  finishedReauthAttempt: false,
};

export default {
  state,
  getters,
  mutations,
  actions,
};

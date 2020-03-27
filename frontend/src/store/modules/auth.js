/*
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import axios from 'axios';
import { SkillsConfiguration } from '@skills/skills-client-vue';
import router from '../../router';

const getters = {
  userInfo(state) {
    return state.userInfo;
  },
  isAuthenticated(state, gettersParam) {
    return (
      state.token !== null
      || gettersParam.isPkiAuthenticated
      || state.localAuth
      || state.oAuthAuth
    ) && state.userInfo !== null;
  },
};

const mutations = {
  authUser(state, authData) {
    state.localAuth = true;
    localStorage.setItem('localAuth', 'true');
    if (authData.token) {
      state.token = authData.token;
      SkillsConfiguration.setAuthToken(state.token);
      localStorage.setItem('token', authData.token);
      localStorage.setItem('expirationDate', authData.expirationDate.getTime().toString());
    }
    axios.defaults.headers.common.Authorization = authData.token;
  },
  oAuth2AuthUser(state) {
    state.oAuthAuth = true;
    localStorage.setItem('oAuthAuth', 'true');
  },
  storeUser(state, userInfo) {
    state.userInfo = userInfo;
  },
  clearAuthData(state) {
    state.token = null;
    state.userInfo = null;
    state.localAuth = false;
    state.oAuthAuth = false;
    SkillsConfiguration.logout();
    localStorage.removeItem('localAuth');
    localStorage.removeItem('oAuthAuth');
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
          if (result) {
            const token = result.headers.authorization;
            let expirationDate;
            if (result.headers.tokenexpirationtimestamp) {
              expirationDate = new Date(Number(result.headers.tokenexpirationtimestamp));
              dispatch('setLogoutTimer', expirationDate);
            }
            commit('authUser', { token, expirationDate });
            dispatch('fetchUser')
              .then(() => {
                resolve(result);
            });
          }
        })
        .catch(error => reject(error));
    });
  },
  login({ commit, dispatch }, authData) {
    return new Promise((resolve, reject) => {
      axios.post('/performLogin', authData, { handleError: false })
        .then((result) => {
          const token = result.headers.authorization;
          let expirationDate;
          if (result.headers.tokenexpirationtimestamp) {
            expirationDate = new Date(Number(result.headers.tokenexpirationtimestamp));
            dispatch('setLogoutTimer', expirationDate);
          }
          commit('authUser', { token, expirationDate });
          dispatch('fetchUser')
            .then(() => {
              resolve(result);
            });
        })
        .catch(error => reject(error));
    });
  },
  oAuth2Login({ commit }, oAuthId) {
    commit('oAuth2AuthUser');
    window.location = `/oauth2/authorization/${oAuthId}`;
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
        // (or username/password if being redirected after successful login)
        dispatch('fetchUser', false).then(() => {
          if (state.userInfo) {
            reAuthenticated = true;
            state.localAuth = !getters.isPkiAuthenticated;
          } else {
            // cannot obtain userInfo, so clear any other lingering auth data
            commit('clearAuthData');
          }
          resolve(reAuthenticated);
        }).catch(error => reject(error));
      }
    });
  },
  logout({ commit }) {
    commit('clearAuthData');
    axios.get('/logout')
      .then(() => {
        router.replace('/skills-login');
    });
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
  localAuth: false,
  oAuthAuth: false,
};

export default {
  state,
  getters,
  mutations,
  actions,
};

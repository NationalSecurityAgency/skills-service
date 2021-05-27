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
import Vue from 'vue';
import Vuex from 'vuex';
import axios from 'axios';
import UniqueIdGenerator from '@/common/utilities/UniqueIdGenerator';
import config from './config';

Vue.use(Vuex);

const themeModule = {
  state: {
    progressIndicators: {
      beforeTodayColor: '#14a3d2',
      earnedTodayColor: '#7ed6f3',
      completeColor: '#59ad52',
      incompleteColor: '#cdcdcd',
    },
    charts: {
      axisLabelColor: 'black',
    },
  },
};

export default new Vuex.Store({
  state: {
    authToken: null,
    isAuthenticating: false,
    parentFrame: null,
    version: null,
    themeStyleId: UniqueIdGenerator.uniqueId('custom-theme-style-node-'),
    isSummaryOnly: false,
    softwareVersion: undefined,
    projectId: null,
    serviceUrl: null,
    authenticator: null,
    internalBackButton: true,
  },
  modules: {
    themeModule,
    config,
  },
  mutations: {
    authToken(state, authToken) {
      axios.defaults.headers.common.Authorization = `Bearer ${authToken}`;
      // eslint-disable-next-line no-param-reassign
      state.authToken = authToken;
    },
    parentFrame(state, parentFrame) {
      // eslint-disable-next-line no-param-reassign
      state.parentFrame = parentFrame;
    },
    isAuthenticating(state, isAuthenticating) {
      // eslint-disable-next-line no-param-reassign
      state.isAuthenticating = isAuthenticating;
    },
    version(state, version) {
      // eslint-disable-next-line no-param-reassign
      state.version = version;
    },
    softwareVersion(state, version) {
      // eslint-disable-next-line no-param-reassign
      state.softwareVersion = version;
    },
    isSummaryOnly(state, isSummaryOnly) {
      // eslint-disable-next-line no-param-reassign
      state.isSummaryOnly = isSummaryOnly;
    },
    internalBackButton(state, internalBackButton) {
      // eslint-disable-next-line no-param-reassign
      state.internalBackButton = internalBackButton;
    },
    projectId(state, projectId) {
      // eslint-disable-next-line no-param-reassign
      state.projectId = projectId;
    },
    serviceUrl(state, serviceUrl) {
      // eslint-disable-next-line no-param-reassign
      state.serviceUrl = serviceUrl;
    },
    authenticator(state, authenticator) {
      // eslint-disable-next-line no-param-reassign
      state.authenticator = authenticator;
    },
  },
});

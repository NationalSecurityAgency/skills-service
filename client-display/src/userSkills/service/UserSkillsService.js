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

import store from '@/store';

import 'url-search-params-polyfill';

axios.defaults.withCredentials = true;

export default {
  authenticationUrl: null,

  userId: new URLSearchParams(window.location.search).get('userId'),

  version: null,

  getUserIdParams() {
    if (!this.userId) {
      return {};
    }

    if (typeof this.userId === 'string') {
      return { userId: this.userId };
    }
    return {
      userId: this.userId.id,
      idType: this.userId.idType,
    };
  },

  getUserIdAndVersionParams() {
    const params = this.getUserIdParams();
    params.version = this.version;

    return params;
  },

  getUserSkills() {
    let response = null;
    response = axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/summary`, {
      params: this.getUserIdAndVersionParams(),
    }).then((result) => result.data);
    return response;
  },

  getCustomIconCss() {
    let response = null;
    response = axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/customIconCss`, {
    }).then((result) => result.data);
    return response;
  },

  getCustomGlobalIconCss() {
    let response = null;
    response = axios.get(`${store.state.serviceUrl}/api/icons/customIconCss`, {
    }).then((result) => result.data);
    return response;
  },

  getSubjectSummary(subjectId) {
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/subjects/${subjectId}/summary`, {
      params: this.getUserIdAndVersionParams(),
    }).then((result) => result.data);
  },

  getSkillDependencies(skillId) {
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/skills/${skillId}/dependencies`, {
      params: this.getUserIdParams(),
    }).then((result) => result.data);
  },

  getSkillSummary(skillId, optionalCrossProjectId) {
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/skills/${skillId}/summary`;
    if (optionalCrossProjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/projects/${optionalCrossProjectId}/skills/${skillId}/summary`;
    }
    return axios.get(url, {
      params: this.getUserIdParams(),
      withCredentials: true,
    }).then((result) => result.data);
  },

  getBadgeSkills(badgeId, global) {
    const requestParams = this.getUserIdAndVersionParams();
    requestParams.global = global;
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/badges/${badgeId}/summary`, {
      params: requestParams,
    }).then((result) => result.data);
  },

  getBadgeSummaries() {
    return axios.get(`${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/badges/summary`, {
      params: this.getUserIdAndVersionParams(),
    }).then((result) => result.data);
  },

  getPointsHistory(subjectId) {
    let response = null;
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/subjects/${subjectId}/pointHistory`;
    if (!subjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/pointHistory`;
    }
    response = axios.get(url, {
      params: this.getUserIdAndVersionParams(),
    }).then((result) => result.data);
    return response;
  },

  reportSkill(skillId, approvalRequestedMsg) {
    let response = null;
    response = axios.post(`${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/skills/${skillId}`, {
      params: this.getUserIdAndVersionParams(),
      approvalRequestedMsg,
    }).then((result) => result.data);
    return response;
  },

  getUserSkillsRanking(subjectId) {
    let response = null;
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/subjects/${subjectId}/rank`;
    if (!subjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/rank`;
    }
    response = axios.get(url, {
      params: this.getUserIdParams(),
    }).then((result) => result.data);
    return response;
  },

  getUserSkillsRankingDistribution(subjectId) {
    let response = null;
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/subjects/${subjectId}/rankDistribution`;
    if (!subjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/rankDistribution`;
    }
    const requestParams = this.getUserIdParams();
    requestParams.subjectId = subjectId;
    response = axios.get(url, {
      params: requestParams,
    }).then((result) => result.data);
    return response;
  },

  getRankingDistributionUsersPerLevel(subjectId) {
    let response = null;
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/subjects/${subjectId}/rankDistribution/usersPerLevel`;
    if (!subjectId) {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/rankDistribution/usersPerLevel`;
    }
    response = axios.get(url, {
      params: {
        subjectId,
      },
    }).then((result) => result.data);
    return response;
  },

  getDescriptions(parentId, type = 'subject') {
    let url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/subjects/${parentId}/descriptions`;
    if (type === 'badge' || type === 'global-badge') {
      url = `${store.state.serviceUrl}${this.getServicePath()}/${store.state.projectId}/badges/${parentId}/descriptions`;
    }
    const response = axios.get(url, {
      params: {
        version: this.version,
        global: type === 'global-badge',
      },
    }).then((result) => result.data);
    return response;
  },

  getServicePath() {
    return '/api/projects';
  },

  setVersion(version) {
    store.commit('version', version);
    this.version = version;
  },

  setServiceUrl(serviceUrl) {
    this.serviceUrl = serviceUrl;
  },

  setUserId(userId) {
    this.userId = userId;
  },
};

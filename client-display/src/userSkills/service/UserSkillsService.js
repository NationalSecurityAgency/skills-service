import axios from 'axios';
import router from '@/router.js';
import store from '@/store.js';

import 'url-search-params-polyfill';

axios.defaults.withCredentials = true;

axios.interceptors.response.use((response) => {
  return response;
}, () => {
  console.log('request rejected');
  console.log(router.currentRoute);
  if (store.state.isAuthenticating) {
    // redirect to error
    router.push({
      name: 'error',
      params: {
        errorMessage: 'Authentication Failed',
      },
    });
  } else {
    router.push({
      name: 'authenticate',
      params: {
        targetRoute: router.currentRoute,
      },
    });
  }
});

const service = {
  authenticationUrl: null,

  serviceUrl: null,

  projectId: null,

  token: null,

  userId: new URLSearchParams(window.location.search).get('userId'),

  version: null,

  getAuthenticationToken() {
    return axios.get(this.authenticationUrl).then(result => result.data);
  },

  getUserSkills() {
    let response = null;
    response = axios.get(`${this.serviceUrl}${this.getServicePath()}/${this.projectId}/summary`, {
      params: {
        userId: this.userId,
        version: this.version,
      },
    }).then(result => result.data);
    return response;
  },

  getCustomIconCss() {
    let response = null;
    response = axios.get(`${this.serviceUrl}${this.getServicePath()}/${this.projectId}/customIconCss`, {
    }).then(result => result.data);
    return response;
  },

  getSubjectSummary(subjectId) {
    return axios.get(`${this.serviceUrl}${this.getServicePath()}/${this.projectId}/subjects/${subjectId}/summary`, {
      params: {
        userId: this.userId,
        version: this.version,
      },
    }).then(result => result.data);
  },

  getSkillDependencies(skillId) {
    return axios.get(`${this.serviceUrl}${this.getServicePath()}/${this.projectId}/skills/${skillId}/dependencies`, {
      params: {
        userId: this.userId,
      },
    }).then(result => result.data);
  },

  getSkillSummary(skillId, optionalCrossProjectId) {
    let url = `${this.serviceUrl}${this.getServicePath()}/${this.projectId}/skills/${skillId}/summary`;
    if (optionalCrossProjectId) {
      url = `${this.serviceUrl}${this.getServicePath()}/${this.projectId}/projects/${optionalCrossProjectId}/skills/${skillId}/summary`;
    }
    return axios.get(url, {
      params: {
        userId: this.userId,
      },
      withCredentials: true,
    }).then(result => result.data);
  },

  getBadgeSkills(badgeId) {
    return axios.get(`${this.serviceUrl}${this.getServicePath()}/${this.projectId}/badges/${badgeId}/summary`, {
      params: {
        userId: this.userId,
      },
    }).then(result => result.data);
  },

  getBadgeSummaries() {
    return axios.get(`${this.serviceUrl}${this.getServicePath()}/${this.projectId}/badges/summary`, {
      params: {
        userId: this.userId,
      },
    }).then(result => result.data);
  },

  getPointsHistory(subjectId) {
    let response = null;
    let url = `${this.serviceUrl}${this.getServicePath()}/${this.projectId}/subjects/${subjectId}/pointHistory`;
    if (!subjectId) {
      url = `${this.serviceUrl}${this.getServicePath()}/${this.projectId}/pointHistory`;
    }
    response = axios.get(url, {
      params: {
        userId: this.userId,
      },
    }).then(result => result.data.pointsHistory);
    return response;
  },

  addUserSkill(userSkillId) {
    let response = null;
    response = axios.get(`${this.serviceUrl}${this.getServicePath()}/${this.projectId}/addSkill/${userSkillId}`, {
      params: {
        userId: this.userId,
      },
    }).then(result => result.data);
    return response;
  },

  getUserSkillsRanking(subjectId) {
    let response = null;
    let url = `${this.serviceUrl}${this.getServicePath()}/${this.projectId}/subjects/${subjectId}/rank`;
    if (!subjectId) {
      url = `${this.serviceUrl}${this.getServicePath()}/${this.projectId}/rank`;
    }
    response = axios.get(url, {
      params: {
        userId: this.userId,
      },
    }).then(result => result.data);
    return response;
  },

  getUserSkillsRankingDistribution(subjectId) {
    let response = null;
    let url = `${this.serviceUrl}${this.getServicePath()}/${this.projectId}/subjects/${subjectId}/rankDistribution`;
    if (!subjectId) {
      url = `${this.serviceUrl}${this.getServicePath()}/${this.projectId}/rankDistribution`;
    }
    response = axios.get(url, {
      params: {
        subjectId,
        userId: this.userId,
      },
    }).then(result => result.data);
    return response;
  },

  getServicePath() {
    let servicePath = '/api/projects';
    if (this.userId) {
      servicePath = '/admin/projects';
    }
    return servicePath;
  },

  setAuthenticationUrl(authenticationUrl) {
    this.authenticationUrl = authenticationUrl;
  },

  setServiceUrl(serviceUrl) {
    this.serviceUrl = serviceUrl;
  },

  setProjectId(projectId) {
    this.projectId = projectId;
  },

  setUserId(userId) {
    this.userId = userId;
  },

  setToken(token) {
    this.token = token;
    if (token) {
      axios.defaults.headers.common.Authorization = `Bearer ${token}`;
    } else {
      delete axios.defaults.headers.common.Authorization;
    }
  },
};

export default service;

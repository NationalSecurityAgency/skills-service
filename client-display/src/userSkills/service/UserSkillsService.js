import axios from 'axios';

import 'url-search-params-polyfill';

axios.defaults.withCredentials = true;

export default {
  serviceUrl: null,

  projectId: null,

  token: null,

  userId: new URLSearchParams(window.location.search).get('userId'),

  version: null,

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

  getSkillSummary(skillId) {
    return axios.get(`${this.serviceUrl}${this.getServicePath()}/${this.projectId}/skills/${skillId}/summary`, {
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

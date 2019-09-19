import axios from 'axios';

import SkillsConfiguration from '@skills/skills-client-configuration';
import store from '@/store';

import 'url-search-params-polyfill';

axios.defaults.withCredentials = true;

export default {
  authenticationUrl: null,

  userId: new URLSearchParams(window.location.search).get('userId'),

  version: null,

  getUserSkills() {
    let response = null;
    response = axios.get(`${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/summary`, {
      params: {
        userId: this.userId,
        version: this.version,
      },
    }).then(result => result.data);
    return response;
  },

  getCustomIconCss() {
    let response = null;
    response = axios.get(`${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/customIconCss`, {
    }).then(result => result.data);
    return response;
  },

  getCustomGlobalIconCss() {
    let response = null;
    response = axios.get(`${SkillsConfiguration.getServiceUrl()}/api/icons/customIconCss`, {
    }).then(result => result.data);
    return response;
  },


  getSubjectSummary(subjectId) {
    return axios.get(`${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/subjects/${subjectId}/summary`, {
      params: {
        userId: this.userId,
        version: this.version,
      },
    }).then(result => result.data);
  },

  getSkillDependencies(skillId) {
    return axios.get(`${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/skills/${skillId}/dependencies`, {
      params: {
        userId: this.userId,
      },
    }).then(result => result.data);
  },

  getSkillSummary(skillId, optionalCrossProjectId) {
    let url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/skills/${skillId}/summary`;
    if (optionalCrossProjectId) {
      url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/projects/${optionalCrossProjectId}/skills/${skillId}/summary`;
    }
    return axios.get(url, {
      params: {
        userId: this.userId,
      },
      withCredentials: true,
    }).then(result => result.data);
  },

  getBadgeSkills(badgeId, global) {
    return axios.get(`${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/badges/${badgeId}/summary`, {
      params: {
        userId: this.userId,
        version: this.version,
        global,
      },
    }).then(result => result.data);
  },

  getBadgeSummaries() {
    return axios.get(`${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/badges/summary`, {
      params: {
        userId: this.userId,
        version: this.version,
      },
    }).then(result => result.data);
  },

  getPointsHistory(subjectId) {
    let response = null;
    let url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/subjects/${subjectId}/pointHistory`;
    if (!subjectId) {
      url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/pointHistory`;
    }
    response = axios.get(url, {
      params: {
        userId: this.userId,
        version: this.version,
      },
    }).then(result => result.data.pointsHistory);
    return response;
  },

  addUserSkill(userSkillId) {
    let response = null;
    response = axios.get(`${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/addSkill/${userSkillId}`, {
      params: {
        userId: this.userId,
      },
    }).then(result => result.data);
    return response;
  },

  getUserSkillsRanking(subjectId) {
    let response = null;
    let url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/subjects/${subjectId}/rank`;
    if (!subjectId) {
      url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/rank`;
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
    let url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/subjects/${subjectId}/rankDistribution`;
    if (!subjectId) {
      url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/rankDistribution`;
    }
    response = axios.get(url, {
      params: {
        subjectId,
        userId: this.userId,
      },
    }).then(result => result.data);
    return response;
  },

  getRankingDistributionUsersPerLevel(subjectId) {
    let response = null;
    let url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/subjects/${subjectId}/rankDistribution/usersPerLevel`;
    if (!subjectId) {
      url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/rankDistribution/usersPerLevel`;
    }
    response = axios.get(url, {
      params: {
        subjectId,
      },
    }).then(result => result.data);
    return response;
  },

  getDescriptions(parentId, type = 'subject') {
    let url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/subjects/${parentId}/descriptions`;
    if (type === 'badge') {
      url = `${SkillsConfiguration.getServiceUrl()}${this.getServicePath()}/${SkillsConfiguration.getProjectId()}/badges/${parentId}/descriptions`;
    }
    const response = axios.get(url, {
      params: {
        version: this.version,
      },
    }).then(result => result.data);
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

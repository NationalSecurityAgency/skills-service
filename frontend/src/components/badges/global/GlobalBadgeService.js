import axios from 'axios';

export default {
  getBadges() {
    return axios.get('/supervisor/badges')
      .then(response => response.data);
  },
  getBadge(badgeId) {
    return axios.get(`/supervisor/badges/${badgeId}`)
      .then(response => response.data);
  },
  saveBadge(badgeReq) {
    return axios.put(`/supervisor/badges/${badgeReq.badgeId}`, badgeReq)
      .then(() => this.getBadge(badgeReq.badgeId));
  },
  deleteBadge(badgeId) {
    return axios.delete(`/supervisor/badges/${badgeId}`)
      .then(response => response.data);
  },
  moveBadge(badgeId, actionToSubmit) {
    return axios.patch(`/supervisor/badges/${badgeId}`, {
      action: actionToSubmit,
    })
      .then(response => response.data);
  },
  badgeWithNameExists(badgeName) {
    return axios.get(`/supervisor/badges/name/${encodeURIComponent(badgeName)}/exists`)
      .then(remoteRes => !remoteRes.data);
  },
  badgeWithIdExists(badgeId) {
    return axios.get(`/supervisor/badges/name/${encodeURIComponent(badgeId)}/exists`)
      .then(remoteRes => !remoteRes.data);
  },
  assignSkillToBadge(badgeId, projectId, skillId) {
    return axios.post(`/supervisor/badges/${badgeId}/projects/${projectId}/skills/${skillId}`)
      .then(res => res.data);
  },
  removeSkillFromBadge(badgeId, projectId, skillId) {
    return axios.delete(`/supervisor/badges/${badgeId}/projects/${projectId}/skills/${skillId}`)
      .then(res => res.data);
  },
  getBadgeSkills(badgeId) {
    return axios.get(`/supervisor/badges/${badgeId}/skills`)
      .then(res => res.data);
  },
  suggestProjectSkills(badgeId, search) {
    return axios.get(`/supervisor/badges/${badgeId}/skills/available?query=${search}`)
      .then(res => res.data);
  },
  getAllProjects() {
    return axios.get('/supervisor/projects')
      .then(res => res.data);
  },
  getProjectLevels(projectId) {
    return axios.get(`/supervisor/projects/${projectId}/levels`)
      .then(res => res.data);
  },
  assignProjectLevelToBadge(badgeId, projectId, levelId) {
    return axios.post(`/supervisor/badges/${badgeId}/projects/${projectId}/level/${levelId}`)
      .then(res => res.data);
  },
};

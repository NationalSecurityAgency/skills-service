import axios from 'axios';

export default {
  getBadges(projectId) {
    return axios.get(`/admin/projects/${projectId}/badges`)
      .then(response => response.data);
  },
  getBadge(projectId, badgeId) {
    return axios.get(`/admin/projects/${projectId}/badges/${badgeId}`)
      .then(response => response.data);
  },
  saveBadge(badgeReq) {
    return axios.put(`/admin/projects/${badgeReq.projectId}/badges/${badgeReq.badgeId}`, badgeReq)
      .then(() => this.getBadge(badgeReq.projectId, badgeReq.badgeId));
  },
  deleteBadge(projectId, badgeId) {
    return axios.delete(`/admin/projects/${projectId}/badges/${badgeId}`)
      .then(repsonse => repsonse.data);
  },
  moveBadge(projectId, badgeId, actionToSubmit) {
    return axios.patch(`/admin/projects/${projectId}/badges/${badgeId}`, {
      action: actionToSubmit,
    })
      .then(response => response.data);
  },
  badgeWithNameExists(projectId, badgeName) {
    return axios.get(`/admin/projects/${projectId}/badgeNameExists?badgeName=${encodeURIComponent(badgeName)}`)
      .then(remoteRes => !remoteRes.data);
  },
  badgeWithIdExists(projectId, badgeId) {
    return axios.get(`/admin/projects/${projectId}/entityIdExists?id=${badgeId}`)
      .then(remoteRes => !remoteRes.data);
  },
};

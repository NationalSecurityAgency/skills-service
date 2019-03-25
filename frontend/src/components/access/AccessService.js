import axios from 'axios';

export default {
  getUserRoles(projectId) {
    if (projectId) {
      return axios.get(`/admin/projects/${projectId}/userRoles`)
        .then(response => response.data);
    }
    return axios.get('/root/rootUsers')
      .then(response => response.data);
  },
  saveUserRole(projectId, userDn, roleName) {
    if (projectId) {
      return axios.put(`/admin/projects/${projectId}/users/${userDn}/roles/${roleName}`, {
        userDnVal: userDn,
        projectIdVal: projectId,
        roleNameVal: roleName,
      })
        .then(response => response.data);
    }
    return axios.put(`/root/addRoot/${userDn}`).then(response => response.data);
  },
  deleteUserRole(projectId, userId, roleName) {
    if (projectId) {
      return axios.delete(`/admin/projects/${projectId}/users/${userId}/roles/${encodeURIComponent(roleName)}`);
    }
    return axios.delete(`/root/deleteRoot/${userId}`).then(response => response.data);
  },
  getOAuthProviders() {
    return axios.get('/app/oAuthProviders')
      .then(response => response.data);
  },
  resetClientSecret(projectId) {
    return axios.put(`/admin/projects/${projectId}/resetClientSecret`)
      .then(response => response.data);
  },
};

import axios from 'axios';

export default {
  getUserRoles(projectId) {
    return axios.get(`/admin/projects/${projectId}/userRoles`)
      .then(response => response.data);
  },
  saveUserRole(projectId, userDn, roleName) {
    return axios.put(`/admin/projects/${projectId}/users/${userDn}/roles/${roleName}`, {
      userDnVal: userDn,
      projectIdVal: projectId,
      roleNameVal: roleName,
    })
      .then(response => response.data);
  },
  deleteUserRole(projectId, userId, roleName) {
    return axios.delete(`/admin/projects/${projectId}/users/${userId}/roles/${encodeURIComponent(roleName)}`);
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

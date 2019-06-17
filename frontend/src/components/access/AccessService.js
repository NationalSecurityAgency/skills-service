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
  saveUserRole(projectId, userInfo, roleName, isPkiAuthenticated) {
    const { userId } = userInfo;
    let userKey = userId;
    if (isPkiAuthenticated) {
      userKey = userInfo.dn;
    }
    if (projectId) {
      return axios.put(`/admin/projects/${projectId}/users/${userKey}/roles/${roleName}`)
        .then(() => axios.get(`/admin/projects/${projectId}/users/${userId}/roles`)
          .then(response => response.data.find(element => element.roleName === roleName)));
    }
    return axios.put(`/root/addRoot/${userKey}`)
      .then(() => axios.get('/root/rootUsers')
        .then(response => response.data.find(element => element.userId === userId)));
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
      .then(() => this.getClientSecret(projectId));
  },
  getClientSecret(projectId) {
    return axios.get(`/admin/projects/${projectId}/clientSecret`)
      .then(response => response.data);
  },
  userWithEmailExists(email) {
    return axios.get(`/userExists/${email}`).then(response => !response.data);
  },
};

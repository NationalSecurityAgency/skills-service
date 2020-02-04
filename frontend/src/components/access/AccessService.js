import axios from 'axios';

export default {
  getUserRoles(projectId, roleName) {
    if (projectId) {
      return axios.get(`/admin/projects/${projectId}/userRoles`)
        .then(response => response.data);
    }
    if (roleName === 'ROLE_SUPER_DUPER_USER' || roleName === 'ROLE_SUPERVISOR') {
      return axios.get(`/root/users/roles/${roleName}`)
        .then(response => response.data);
    }
    throw new Error(`unexpected user role [${roleName}]`);
  },
  saveUserRole(projectId, userInfo, roleName, isPkiAuthenticated) {
    let { userId } = userInfo;
    let userKey = userId;
    if (isPkiAuthenticated) {
      userKey = userInfo.dn;
      userId = userKey;
    }
    if (projectId) {
      return axios.put(`/admin/projects/${projectId}/users/${userKey}/roles/${roleName}`, null, { headers:{'x-handleError': false }})
        .then(() => axios.get(`/admin/projects/${projectId}/users/${userId}/roles`)
          .then(response => response.data.find(element => element.roleName === roleName)));
    }
    if (roleName === 'ROLE_SUPER_DUPER_USER' || roleName === 'ROLE_SUPERVISOR') {
      return axios.put(`/root/users/${userKey}/roles/${roleName}`)
        .then(() => axios.get(`/root/users/roles/${roleName}`)
          .then(response => response.data.find(element => element.userId.toLowerCase() === userId.toLowerCase())));
    }
    throw new Error(`unexpected user role [${roleName}]`);
  },
  deleteUserRole(projectId, userId, roleName) {
    if (projectId) {
      return axios.delete(`/admin/projects/${projectId}/users/${userId}/roles/${encodeURIComponent(roleName)}`);
    }
    if (roleName === 'ROLE_SUPER_DUPER_USER' || roleName === 'ROLE_SUPERVISOR') {
      return axios.delete(`/root/users/${userId}/roles/${roleName}`).then(response => response.data);
    }
    throw new Error(`unexpected user role [${roleName}]`);
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
  hasRole(roleName) {
    return axios.get(`/app/userInfo/hasRole/${roleName}`).then(response => response.data);
  },
};

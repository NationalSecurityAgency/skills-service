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
    const origUserId = userId;
    if (isPkiAuthenticated) {
      userKey = userInfo.dn;
      userId = userKey;
    }
    if (projectId) {
      return axios.put(`/admin/projects/${projectId}/users/${userKey}/roles/${roleName}`, null, { handleError: false })
        .then(() => axios.get(`/admin/projects/${projectId}/users/${userId}/roles`)
          .then(response => response.data.find(element => element.roleName === roleName)));
    }
    if (roleName === 'ROLE_SUPER_DUPER_USER' || roleName === 'ROLE_SUPERVISOR') {
      return axios.put(`/root/users/${userKey}/roles/${roleName}`, null, { handleError: false })
        .then(() => axios.get(`/root/users/roles/${roleName}`)
          .then(response => response.data.find(element => element.userIdForDisplay.toLowerCase() === origUserId.toLowerCase())));
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
  requestPasswordReset(userId) {
    const formData = new FormData();
    formData.append('userId', userId);
    return axios.post('/resetPassword', formData, { handleError: false }).then(response => response.data);
  },
  resetPassword(reset) {
    return axios.post('/performPasswordReset', reset, { handleError: false }).then(response => response.data);
  },
  isResetSupported() {
    return axios.get('/isFeatureSupported?feature=passwordreset').then(response => response.data);
  },
};

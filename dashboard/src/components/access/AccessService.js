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
import axios from 'axios'

const supportedRoles = ['ROLE_SUPER_DUPER_USER', 'ROLE_SUPERVISOR', 'ROLE_DASHBOARD_ADMIN_ACCESS', 'ROLE_ADMIN_GROUP_MEMBER', 'ROLE_ADMIN_GROUP_OWNER']
const hasSupportedRole = (roles) => {
  if (roles instanceof Array) {
    return roles.some(value => supportedRoles.includes(value));
  }
  return supportedRoles.includes(roles);
}
export default {

  countUserRolesForProject(projectId, roles) {
    const strRoles = roles.map((r) => `roles=${encodeURIComponent(r)}`).join('&')
      return axios
          .get(`/admin/projects/${encodeURIComponent(projectId)}/countUserRoles?${strRoles}`)
          .then((response) => response.data)
  },
  getUserRoles(projectId, roles, params, adminGroupId) {
    const strRoles = roles.map((r) => `roles=${encodeURIComponent(r)}`).join('&')
    if (projectId) {
      return axios
        .get(`/admin/projects/${encodeURIComponent(projectId)}/userRoles?${strRoles}`, { params })
        .then((response) => response.data)
    }
    if (adminGroupId) {
      return axios
          .get(`/admin/admin-group-definitions/${adminGroupId}/userRoles?${strRoles}`, { params })
          .then((response) => response.data)
    }
    if (roles.length === 1 && hasSupportedRole(roles)) {
      return axios
        .get(`/root/users/roles/${roles[0]}`, { params })
        .then((response) => response.data)
    }
    throw new Error(`unexpected user roles [${params.roles}]`)
  },
  saveUserRole(projectId, userInfo, roleName, isPkiAuthenticated, adminGroupId) {
    let { userId } = userInfo
    let userKey = userId
    if (isPkiAuthenticated) {
      userKey = userInfo.dn
      userId = userKey
    }
    if (projectId) {
      return axios.put(
        `/admin/projects/${encodeURIComponent(projectId)}/users/${userKey}/roles/${roleName}`,
        null,
        { handleError: false }
      )
    }
    if (adminGroupId) {
      return axios.put(
          `/admin/admin-group-definitions/${encodeURIComponent(adminGroupId)}/users/${userKey}/roles/${roleName}`,
          null,
          { handleError: false }
      )
    }
    if (hasSupportedRole(roleName)) {
      return axios.put(`/root/users/${userKey}/roles/${roleName}`, null, { handleError: false })
    }
    throw new Error(`unexpected user role [${roleName}]`)
  },
  deleteUserRole(projectId, userId, roleName, adminGroupId) {
    if (projectId) {
      return axios.delete(
        `/admin/projects/${encodeURIComponent(projectId)}/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleName)}`
      )
    }
    if (adminGroupId) {
      return axios.delete(
          `/admin/admin-group-definitions/${encodeURIComponent(projectId)}/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleName)}`
      )
    }
    if (hasSupportedRole(roleName)) {
      return axios
        .delete(`/root/users/${encodeURIComponent(userId)}/roles/${roleName}`)
        .then((response) => response.data)
    }
    throw new Error(`unexpected user role [${roleName}]`)
  },
  getOAuthProviders() {
    return axios.get('/app/oAuthProviders').then((response) => response.data)
  },
  resetClientSecret(projectId) {
    return axios
      .put(`/admin/projects/${encodeURIComponent(projectId)}/resetClientSecret`)
      .then(() => this.getClientSecret(projectId))
  },
  getClientSecret(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/clientSecret`)
      .then((response) => response.data)
  },
  userWithEmailExists(email) {
    return axios.get(`/userExists/${encodeURIComponent(email)}`).then((response) => !response.data)
  },
  hasRole(roleName) {
    return axios.get(`/app/userInfo/hasRole/${roleName}`).then((response) => response.data)
  },
  requestPasswordReset(userId) {
    const formData = new FormData()
    formData.append('userId', userId)
    return axios
      .post('/resetPassword', formData, { handleError: false })
      .then((response) => response.data)
  },
  resetPassword(reset) {
    return axios
      .post('/performPasswordReset', reset, { handleError: false })
      .then((response) => response.data)
  },
  isResetSupported() {
    return axios
      .get('/public/isFeatureSupported?feature=passwordreset')
      .then((response) => response.data)
  },
  getUserAgreement() {
    return axios.get('/app/userAgreement').then((response) => response.data)
  },
  verifyEmail(verification) {
    return axios
      .post('/verifyEmail', verification, { handleError: false })
      .then((response) => response.data)
  },
  resendEmailVerification(userId) {
    return axios
      .post(`resendEmailVerification/${encodeURIComponent(userId)}`)
      .then((response) => response.data)
  },
  userEmailIsVerified(email) {
    return axios.get(`/userEmailIsVerified/${email}`).then((response) => response.data)
  },
  sendProjectInvites(projectId, inviteRequest) {
    return axios
      .post(`/admin/projects/${projectId}/invite`, inviteRequest)
      .then((response) => response.data)
  },
  isInviteValid(projectId, inviteToken) {
    return axios
      .get(`/app/projects/${projectId}/validateInvite/${inviteToken}`)
      .then((response) => response.data)
  },
  joinProject(projectId, inviteToken) {
    return axios
      .post(`/app/projects/${encodeURIComponent(projectId)}/join/${inviteToken}`)
      .then((response) => response.data)
  },
  getUserRolesForProject(projectId, roleName, params) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/userRoles/${encodeURIComponent(roleName)}`,
        { params }
      )
      .then((resp) => resp.data)
  },
  getInviteStatuses(projectId, recipientQuery, params) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/invites/status?query=${recipientQuery}`,
        { params }
      )
      .then((resp) => resp.data)
  },
  extendInvite(projectId, recipientEmail, iso8601Duration) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(projectId)}/invites/extend`, {
        extensionDuration: iso8601Duration,
        recipientEmail
      })
      .then((resp) => resp.data)
  },
  deleteInvite(projectId, recipientEmail) {
    return axios
      .delete(
        `/admin/projects/${encodeURIComponent(projectId)}/invites/${encodeURIComponent(recipientEmail)}`
      )
      .then((resp) => resp.data)
  },
  remindInvitedUser(projectId, recipientEmail) {
    return axios
      .post(
        `/admin/projects/${encodeURIComponent(projectId)}/invites/${encodeURIComponent(recipientEmail)}/remind`,
        null,
        { handleError: false }
      )
      .then((resp) => resp.data)
  },
  suggestUsers(query, suggestUrl) {
    return axios
      .post(suggestUrl, {
        suggestQuery: query ? query : ''
      })
      .then((resp) => resp.data)
  }
}

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

export default {
  getSetting(projectId, settingName) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/settings/${settingName}`)
      .then((remoteRes) => remoteRes.data)
  },
  saveSettings(projectId, settings) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(projectId)}/settings`, settings)
      .then((response) => response.data)
  },
  saveGlobalSettings(settings) {
    return axios.post('/root/global/settings', settings).then((response) => response.data)
  },
  getGlobalSettings(settingGroup) {
    return axios.get(`/root/global/settings/${settingGroup}`).then((response) => response.data)
  },
  checkSettingsValidity(projectId, settings) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(projectId)}/settings/checkValidity`, settings)
      .then((response) => response.data)
  },
  getSettingsForProject(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/settings`)
      .then((remoteRes) => remoteRes.data)
  },
  getProjectSetting(projectId, setting, handleError = true) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/settings/${setting}`, { handleError })
      .then((remoteRes) => remoteRes.data)
  },
  getUserSettings() {
    return axios.get('/app/userInfo/settings').then((remoteRes) => remoteRes.data)
  },
  saveUserSettings(settings) {
    return axios.post('/app/userInfo/settings', settings).then((response) => response.data)
  },
  checkUserSettingsValidity(settings) {
    return axios
      .post('/app/userInfo/settings/checkValidity', settings)
      .then((response) => response.data)
  },
  hasRoot() {
    return axios.get('/root/isRoot').then((response) => response.data)
  },
  testConnection(emailConnectionInfo) {
    return axios
      .post('/root/testEmailSettings', emailConnectionInfo)
      .then((response) => response.data)
  },
  saveEmailSettings(emailConnectionInfo) {
    return axios
      .post('/root/saveEmailSettings', emailConnectionInfo, { handleError: false })
      .then((response) => response.data)
  },
  loadEmailSettings() {
    return axios.get('/root/getEmailSettings').then((response) => response.data)
  },
  saveSystemSettings(generalSettings) {
    return axios.post('/root/saveSystemSettings', generalSettings).then((response) => response.data)
  },
  loadSystemSettings() {
    return axios.get('/root/getSystemSettings').then((response) => response.data)
  },
  pinProject(projectId) {
    return axios
      .post(`/root/pin/${encodeURIComponent(projectId)}`)
      .then((response) => response.data)
  },
  unpinProject(projectId) {
    return axios
      .delete(`/root/pin/${encodeURIComponent(projectId)}`)
      .then((response) => response.data)
  },
  saveUserInfo(userInfo) {
    return axios.post('/app/userInfo', userInfo).then(() => this.getUserInfo())
  },
  getUserInfo() {
    return axios.get('/app/userInfo').then((response) => response.data)
  },
  getPublicSettings(settingGroup) {
    return axios
      .get(`/app/public/settings/group/${settingGroup}`)
      .then((remoteRes) => remoteRes.data)
  },
  getPublicSetting(settingName, settingGroup) {
    return axios
      .get(`/app/public/settings/${settingName}/group/${settingGroup}`)
      .then((remoteRes) => remoteRes.data)
  },
  getConfig() {
    return axios.get('/public/config').then((remoteRes) => remoteRes.data)
  },
  getClientDisplayConfig(projectId) {
    return axios
      .get(`/public/clientDisplay/config?projectId=${encodeURIComponent(projectId)}`)
      .then((remoteRes) => remoteRes.data)
  },
  isEmailServiceSupported() {
    return axios
      .get('/public/isFeatureSupported?feature=emailservice')
      .then((response) => response.data)
  }
}

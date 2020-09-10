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
  getSetting(projectId, settingName) {
    return axios.get(`/admin/projects/${projectId}/settings/${settingName}`)
      .then((remoteRes) => remoteRes.data);
  },
  saveSettings(projectId, settings) {
    return axios.post(`/admin/projects/${projectId}/settings`, settings).then((response) => response.data);
  },
  checkSettingsValidity(projectId, settings) {
    return axios.post(`/admin/projects/${projectId}/settings/checkValidity`, settings).then((response) => response.data);
  },
  getSettingsForProject(projectId) {
    return axios.get(`/admin/projects/${projectId}/settings`)
      .then((remoteRes) => remoteRes.data);
  },
  hasRoot() {
    return axios.get('/root/isRoot').then((response) => response.data);
  },
  testConnection(emailConnectionInfo) {
    return axios.post('/root/testEmailSettings', emailConnectionInfo).then((response) => response.data);
  },
  saveEmailSettings(emailConnectionInfo) {
    return axios.post('/root/saveEmailSettings', emailConnectionInfo, { handleError: false }).then((response) => response.data);
  },
  loadEmailSettings() {
    return axios.get('/root/getEmailSettings').then((response) => response.data);
  },
  saveSystemSettings(generalSettings) {
    return axios.post('/root/saveSystemSettings', generalSettings).then((response) => response.data);
  },
  loadSystemSettings() {
    return axios.get('/root/getSystemSettings').then((response) => response.data);
  },
  saveUserInfo(userInfo) {
    return axios.post('/app/userInfo', userInfo).then(() => this.getUserInfo());
  },
  getUserInfo() {
    return axios.get('/app/userInfo').then((response) => response.data);
  },
  getPublicSettings(settingGroup) {
    return axios.get(`/app/public/settings/group/${settingGroup}`)
      .then((remoteRes) => remoteRes.data);
  },
  getPublicSetting(settingName, settingGroup) {
    return axios.get(`/app/public/settings/${settingName}/group/${settingGroup}`)
      .then((remoteRes) => remoteRes.data);
  },
  getConfig() {
    return axios.get('/public/config')
      .then((remoteRes) => remoteRes.data);
  },
};

import axios from 'axios';

export default {
  getSetting(projectId, settingName) {
    return axios.get(`/admin/projects/${projectId}/settings/${settingName}`)
      .then(remoteRes => remoteRes.data);
  },
  saveSettings(projectId, settings) {
    return axios.post(`/admin/projects/${projectId}/settings`, settings).then(response => response.data);
  },
  checkSettingsValidity(projectId, settings) {
    return axios.post(`/admin/projects/${projectId}/settings/checkValidity`, settings).then(response => response.data);
  },
  getSettingsForProject(projectId) {
    return axios.get(`/admin/projects/${projectId}/settings`)
      .then(remoteRes => remoteRes.data);
  },
  hasRoot() {
    return axios.get('/root/isRoot').then(response => response.data);
  },
  testConnection(emailConnectionInfo) {
    return axios.post('/root/testEmailSettings', emailConnectionInfo).then(response => response.data);
  },
  saveEmailSettings(emailConnectionInfo) {
    return axios.post('/root/saveEmailSettings', emailConnectionInfo).then(response => response.data);
  },
  saveUserInfo(userInfo) {
    return axios.post('/app/userInfo', userInfo).then(() => this.getUserInfo());
  },
  getUserInfo() {
    return axios.get('/app/userInfo').then(response => response.data);
  },
  getPublicSettings(settingGroup) {
    return axios.get(`/app/public/settings/group/${settingGroup}`)
      .then(remoteRes => remoteRes.data);
  },
  getPublicSetting(settingName, settingGroup) {
    return axios.get(`/app/public/settings/${settingName}/group/${settingGroup}`)
      .then(remoteRes => remoteRes.data);
  },
  getConfig() {
    return axios.get('/public/config')
      .then(remoteRes => remoteRes.data);
  },
};

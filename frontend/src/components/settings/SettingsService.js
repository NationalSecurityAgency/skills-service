import axios from 'axios';

export default {
  getSetting(projectId, settingName) {
    return axios.get(`/admin/projects/${projectId}/settings/${settingName}`)
      .then(remoteRes => remoteRes.data);
  },
  saveSetting(projectId, settingObj) {
    return axios.post(`/admin/projects/${projectId}/settings/${settingObj.setting}`, settingObj, { handleError: false })
      .then(() => this.getSetting(projectId, settingObj.setting));
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
};

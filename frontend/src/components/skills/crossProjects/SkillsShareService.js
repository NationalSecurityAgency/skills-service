import axios from 'axios';

export default {
  shareSkillToAnotherProject(projectId, skillId, shareToProjectId) {
    return axios.put(`/admin/projects/${projectId}/skills/${skillId}/shared/projects/${shareToProjectId}`);
  },
  deleteSkillShare(projectId, skillId, shareToProjectId) {
    return axios.delete(`/admin/projects/${projectId}/skills/${skillId}/shared/projects/${shareToProjectId}`);
  },
  getSharedSkills(projectId) {
    return axios.get(`/admin/projects/${projectId}/shared`)
      .then(response => response.data);
  },
};

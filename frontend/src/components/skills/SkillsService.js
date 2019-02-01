import axios from 'axios';

export default {
  getSkillDetails(projectId, subjectId, skillId) {
    return axios.get(`/admin/projects/${projectId}/subjects/${subjectId}/skills/${skillId}`)
      .then(response => response.data);
  },
  getSubjectSkills(projectId, subjectId) {
    return axios.get(`/admin/projects/${projectId}/subjects/${subjectId}/skills`)
      .then(response => response.data);
  },
  getProjectSkills(projectId) {
    return axios.get(`/admin/projects/${projectId}/skills`)
      .then(response => response.data);
  },
  saveSkill(skill) {
    return axios.put(`/admin/projects/${skill.projectId}/subjects/${skill.subjectId}/skills/${skill.skillId}`, skill)
      .then(createdRuleResult => createdRuleResult.data);
  },
  deleteSkill(skill) {
    return axios.delete(`/admin/projects/${skill.projectId}/subjects/${skill.subjectId}/skills/${skill.skillId}`)
      .then(res => res.data);
  },
  updateSkill(skill, actionToSubmit) {
    return axios.patch(`/admin/projects/${skill.projectId}/subjects/${skill.subjectId}/skills/${skill.skillId}`, {
      action: actionToSubmit,
    })
      .then(res => res.data);
  },
  getDependentSkillsGraphForSkill(projectId, skillId) {
    return axios.get(`/admin/projects/${projectId}/skills/${skillId}/dependency/graph`)
      .then(res => res.data);
  },
  getDependentSkillsGraphForProject(projectId) {
    return axios.get(`/admin/projects/${projectId}/dependency/graph`)
      .then(res => res.data);
  },
  assignDependency(projectId, skillId, dependentSkillId) {
    return axios.post(`/admin/projects/${projectId}/skills/${skillId}/dependency/${dependentSkillId}`)
      .then(createdRuleResult => createdRuleResult.data);
  },
  removeDependency(projectId, skillId, dependentSkillId) {
    return axios.delete(`/admin/projects/${projectId}/skills/${skillId}/dependency/${dependentSkillId}`)
      .then(createdRuleResult => createdRuleResult.data);
  },
  skillWithNameExists(projectId, skillName) {
    return axios.get(`/admin/projects/${projectId}/skillExists?skillName=${skillName}`)
      .then(remoteRes => !remoteRes.data);
  },
  skillWithIdExists(projectId, skillId) {
    return axios.get(`/admin/projects/${projectId}/skillExists?skillId=${skillId}`)
      .then(remoteRes => !remoteRes.data);
  },
};

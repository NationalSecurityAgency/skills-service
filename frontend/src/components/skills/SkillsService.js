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
  assignSkillToBadge(projectId, badgeId, skillId) {
    return axios.post(`/admin/projects/${projectId}/badge/${badgeId}/skills/${skillId}`)
      .then(res => res.data);
  },
  removeSkillFromBadge(projectId, badgeId, skillId) {
    return axios.delete(`/admin/projects/${projectId}/badge/${badgeId}/skills/${skillId}`)
      .then(res => res.data);
  },
  getBadgeSkills(projectId, badgeId) {
    return axios.get(`/admin/projects/${projectId}/badge/${badgeId}/skills`)
      .then(res => res.data);
  },
  getSkillsFroDependency(projectId) {
    return axios.get(`/admin/projects/${projectId}/dependency/availableSkills`)
      .then(res => res.data);
  },
  assignDependency(projectId, skillId, dependentSkillId, dependentProjectId) {
    if (dependentProjectId) {
      return axios.post(`/admin/projects/${projectId}/skills/${skillId}/dependency/projects/${dependentProjectId}/skills/${dependentSkillId}`)
        .then(createdRuleResult => createdRuleResult.data);
    }
    return axios.post(`/admin/projects/${projectId}/skills/${skillId}/dependency/${dependentSkillId}`)
      .then(createdRuleResult => createdRuleResult.data);
  },
  removeDependency(projectId, skillId, dependentSkillId, dependentProjectId) {
    if (dependentProjectId) {
      return axios.delete(`/admin/projects/${projectId}/skills/${skillId}/dependency/projects/${dependentProjectId}/skills/${dependentSkillId}`)
        .then(createdRuleResult => createdRuleResult.data);
    }
    return axios.delete(`/admin/projects/${projectId}/skills/${skillId}/dependency/${dependentSkillId}`)
      .then(createdRuleResult => createdRuleResult.data);
  },
  skillWithNameExists(projectId, skillName) {
    return axios.get(`/admin/projects/${projectId}/skillExists?skillName=${encodeURIComponent(skillName)}`)
      .then(remoteRes => !remoteRes.data);
  },
  skillWithIdExists(projectId, skillId) {
    return axios.get(`/admin/projects/${projectId}/skillExists?skillId=${skillId}`)
      .then(remoteRes => !remoteRes.data);
  },
  getLatestSkillVersion(projectId) {
    return axios.get(`/admin/projects/${projectId}/latestVersion`)
      .then(remoteRes => remoteRes.data);
  },
};

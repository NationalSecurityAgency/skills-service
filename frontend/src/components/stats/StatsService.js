import axios from 'axios';

export default {
  getUsage(projectId, numDays) {
    return axios.get(`/admin/projects/${projectId}/usage?numDays=${numDays}`)
      .then(response => response.data);
  },
  numAchievedSkillsPivotedBySubject(projectId) {
    return axios.get(`/admin/projects/${projectId}/stats?type=NUM_ACHIEVED_SKILLS_PIVOTED_BY_SUBJECT`)
      .then(response => response.data);
  },
  numUsersPerSkillLevel(projectId) {
    return axios.get(`/admin/projects/${projectId}/stats?type=NUM_USERS_PER_OVERALL_SKILL_LEVEL`)
      .then(response => response.data);
  },
};

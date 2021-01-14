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
  getSkillDetails(projectId, subjectId, skillId) {
    return axios.get(`/admin/projects/${projectId}/subjects/${subjectId}/skills/${skillId}`)
      .then((response) => {
        const skill = response.data;
        return this.enhanceWithTimeWindow(skill);
      });
  },
  enhanceWithTimeWindow(skill) {
    const copy = { ...skill };

    copy.timeWindowEnabled = skill.pointIncrementInterval > 0;
    if (!copy.timeWindowEnabled) {
      // set to default if window is disabled
      copy.pointIncrementIntervalHrs = 8;
      copy.pointIncrementIntervalMins = 0;
    } else {
      copy.pointIncrementIntervalHrs = Math.floor(skill.pointIncrementInterval / 60);
      copy.pointIncrementIntervalMins = skill.pointIncrementInterval % 60;
    }
    copy.numPointIncrementMaxOccurrences = skill.numMaxOccurrencesIncrementInterval;

    return copy;
  },
  getSubjectSkills(projectId, subjectId) {
    return axios.get(`/admin/projects/${projectId}/subjects/${subjectId}/skills`)
      .then((response) => response.data);
  },
  getProjectSkills(projectId) {
    return axios.get(`/admin/projects/${projectId}/skills`)
      .then((response) => response.data);
  },
  saveSkill(skill) {
    const copy = { ...skill };
    if (!skill.timeWindowEnabled) {
      copy.pointIncrementInterval = 0;
    } else {
      // convert to minutes
      copy.pointIncrementInterval = ((parseInt(skill.pointIncrementIntervalHrs, 10) * 60) + parseInt(skill.pointIncrementIntervalMins, 10));
    }
    copy.numMaxOccurrencesIncrementInterval = skill.numPointIncrementMaxOccurrences;

    let requestSkillId = skill.skillId;
    if (skill.isEdit) {
      requestSkillId = skill.originalSkillId;
    }
    return axios.post(`/admin/projects/${skill.projectId}/subjects/${skill.subjectId}/skills/${requestSkillId}`, copy)
      .then(() => this.getSkillDetails(skill.projectId, skill.subjectId, skill.skillId));
  },
  deleteSkill(skill) {
    return axios.delete(`/admin/projects/${skill.projectId}/subjects/${skill.subjectId}/skills/${skill.skillId}`)
      .then((res) => res.data);
  },
  updateSkill(skill, actionToSubmit) {
    return axios.patch(`/admin/projects/${skill.projectId}/subjects/${skill.subjectId}/skills/${skill.skillId}`, {
      action: actionToSubmit,
    })
      .then(() => this.getSkillDetails(skill.projectId, skill.subjectId, skill.skillId));
  },
  getDependentSkillsGraphForSkill(projectId, skillId) {
    return axios.get(`/admin/projects/${projectId}/skills/${skillId}/dependency/graph`)
      .then((res) => res.data);
  },
  getDependentSkillsGraphForProject(projectId) {
    return axios.get(`/admin/projects/${projectId}/dependency/graph`)
      .then((res) => res.data);
  },
  assignSkillToBadge(projectId, badgeId, skillId) {
    return axios.post(`/admin/projects/${projectId}/badge/${badgeId}/skills/${skillId}`)
      .then((res) => res.data);
  },
  removeSkillFromBadge(projectId, badgeId, skillId) {
    return axios.delete(`/admin/projects/${projectId}/badge/${badgeId}/skills/${skillId}`)
      .then((res) => res.data);
  },
  getBadgeSkills(projectId, badgeId) {
    return axios.get(`/admin/projects/${projectId}/badge/${badgeId}/skills`)
      .then((res) => res.data);
  },
  getSkillsFroDependency(projectId) {
    return axios.get(`/admin/projects/${projectId}/dependency/availableSkills`)
      .then((res) => res.data);
  },
  assignDependency(projectId, skillId, dependentSkillId, dependentProjectId) {
    if (dependentProjectId) {
      return axios.post(`/admin/projects/${projectId}/skills/${skillId}/dependency/projects/${dependentProjectId}/skills/${dependentSkillId}`, null, { handleError: false })
        .then((createdRuleResult) => createdRuleResult.data);
    }
    return axios.post(`/admin/projects/${projectId}/skills/${skillId}/dependency/${dependentSkillId}`, null, { handleError: false })
      .then((createdRuleResult) => createdRuleResult.data);
  },
  removeDependency(projectId, skillId, dependentSkillId, dependentProjectId) {
    if (dependentProjectId) {
      return axios.delete(`/admin/projects/${projectId}/skills/${skillId}/dependency/projects/${dependentProjectId}/skills/${dependentSkillId}`)
        .then((createdRuleResult) => createdRuleResult.data);
    }
    return axios.delete(`/admin/projects/${projectId}/skills/${skillId}/dependency/${dependentSkillId}`)
      .then((createdRuleResult) => createdRuleResult.data);
  },
  skillWithNameExists(projectId, skillName) {
    return axios.post(`/admin/projects/${projectId}/skillNameExists`, { name: skillName })
      .then((remoteRes) => !remoteRes.data);
  },
  skillWithIdExists(projectId, skillId) {
    return axios.get(`/admin/projects/${projectId}/entityIdExists?id=${skillId}`)
      .then((remoteRes) => !remoteRes.data);
  },
  getLatestSkillVersion(projectId) {
    return axios.get(`/admin/projects/${projectId}/latestVersion`)
      .then((remoteRes) => remoteRes.data);
  },
  saveSkillEvent(projectId, skillId, user, timestamp) {
    const userId = user.dn ? user.dn : user.userId;
    return axios.put(`/api/projects/${projectId}/skills/${skillId}`, { userId, timestamp }, { handleError: false })
      .then((remoteRes) => remoteRes.data);
  },
  checkIfSkillBelongsToGlobalBadge(projectId, skillId) {
    return axios.get(`/admin/projects/${projectId}/skills/${skillId}/globalBadge/exists`)
      .then((response) => response.data);
  },
};

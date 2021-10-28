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

const enrichSkillObjWithRequiredAtts = (skill) => {
  const copy = { ...skill };
  if (!skill.timeWindowEnabled) {
    copy.pointIncrementInterval = 0;
  } else {
    // convert to minutes
    copy.pointIncrementInterval = ((parseInt(skill.pointIncrementIntervalHrs, 10) * 60) + parseInt(skill.pointIncrementIntervalMins, 10));
  }
  copy.numMaxOccurrencesIncrementInterval = skill.numPointIncrementMaxOccurrences;

  return copy;
};

export default {
  getSkillDetails(projectId, subjectId, skillId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}/skills/${encodeURIComponent(skillId)}`)
      .then((response) => {
        const skill = response.data;
        skill.subjectId = subjectId;
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
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}/skills`)
      .then((response) => response.data);
  },
  getProjectSkills(projectId, skillNameQuery = null) {
    const query = skillNameQuery ? `?skillNameQuery=${encodeURIComponent(skillNameQuery)}` : '';
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/skills${query}`)
      .then((response) => response.data);
  },
  getGroupSkills(projectId, groupId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/groups/${encodeURIComponent(groupId)}/skills`)
      .then((response) => response.data.map((item) => ({ ...item, groupId })));
  },
  saveSkill(skill) {
    const copy = enrichSkillObjWithRequiredAtts(skill);
    let requestSkillId = skill.skillId;
    if (skill.isEdit) {
      requestSkillId = skill.originalSkillId;
    }
    const url = (skill.groupId && skill.groupId.length > 0)
      ? `/admin/projects/${encodeURIComponent(skill.projectId)}/subjects/${encodeURIComponent(skill.subjectId)}/groups/${encodeURIComponent(skill.groupId)}/skills/${encodeURIComponent(requestSkillId)}`
      : `/admin/projects/${encodeURIComponent(skill.projectId)}/subjects/${encodeURIComponent(skill.subjectId)}/skills/${encodeURIComponent(requestSkillId)}`;

    return axios.post(url, copy)
      .then(() => this.getSkillDetails(skill.projectId, skill.subjectId, skill.skillId));
  },
  syncSkillsPoints(projectId, subjectId, groupId, skillsPointsSyncRequest) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}/groups/${encodeURIComponent(groupId)}/skills`;
    return axios.patch(url, skillsPointsSyncRequest)
      .then(() => this.getGroupSkills(projectId, groupId));
  },
  deleteSkill(skill) {
    return axios.delete(`/admin/projects/${encodeURIComponent(skill.projectId)}/subjects/${encodeURIComponent(skill.subjectId)}/skills/${encodeURIComponent(skill.skillId)}`)
      .then((res) => res.data);
  },
  updateSkill(skill, actionToSubmit) {
    return axios.patch(`/admin/projects/${encodeURIComponent(skill.projectId)}/subjects/${encodeURIComponent(skill.subjectId)}/skills/${encodeURIComponent(skill.skillId)}`, {
      action: actionToSubmit,
    })
      .then(() => this.getSkillDetails(skill.projectId, skill.subjectId, skill.skillId));
  },
  getDependentSkillsGraphForSkill(projectId, skillId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/dependency/graph`)
      .then((res) => res.data);
  },
  getDependentSkillsGraphForProject(projectId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/dependency/graph`)
      .then((res) => res.data);
  },
  assignSkillToBadge(projectId, badgeId, skillId) {
    return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/badge/${encodeURIComponent(badgeId)}/skills/${encodeURIComponent(skillId)}`)
      .then((res) => res.data);
  },
  removeSkillFromBadge(projectId, badgeId, skillId) {
    return axios.delete(`/admin/projects/${encodeURIComponent(projectId)}/badge/${encodeURIComponent(badgeId)}/skills/${encodeURIComponent(skillId)}`)
      .then((res) => res.data);
  },
  getBadgeSkills(projectId, badgeId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/badge/${encodeURIComponent(badgeId)}/skills`)
      .then((res) => res.data);
  },
  getSkillsFroDependency(projectId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/dependency/availableSkills`)
      .then((res) => res.data);
  },
  assignDependency(projectId, skillId, dependentSkillId, dependentProjectId) {
    if (dependentProjectId) {
      return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/dependency/projects/${encodeURIComponent(dependentProjectId)}/skills/${encodeURIComponent(dependentSkillId)}`, null, { handleError: false })
        .then((createdRuleResult) => createdRuleResult.data);
    }
    return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/dependency/${encodeURIComponent(dependentSkillId)}`, null, { handleError: false })
      .then((createdRuleResult) => createdRuleResult.data);
  },
  removeDependency(projectId, skillId, dependentSkillId, dependentProjectId) {
    if (dependentProjectId) {
      return axios.delete(`/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/dependency/projects/${encodeURIComponent(dependentProjectId)}/skills/${encodeURIComponent(dependentSkillId)}`)
        .then((createdRuleResult) => createdRuleResult.data);
    }
    return axios.delete(`/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/dependency/${encodeURIComponent(dependentSkillId)}`)
      .then((createdRuleResult) => createdRuleResult.data);
  },
  skillWithNameExists(projectId, skillName) {
    return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/skillNameExists`, { name: skillName })
      .then((remoteRes) => !remoteRes.data);
  },
  skillWithIdExists(projectId, skillId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/entityIdExists?id=${encodeURIComponent(skillId)}`)
      .then((remoteRes) => !remoteRes.data);
  },
  getLatestSkillVersion(projectId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/latestVersion`)
      .then((remoteRes) => remoteRes.data);
  },
  saveSkillEvent(projectId, skillId, user, timestamp) {
    const userId = user.dn ? user.dn : user.userId;
    return axios.put(`/api/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}`, { userId, timestamp }, { handleError: false })
      .then((remoteRes) => remoteRes.data);
  },
  checkIfSkillBelongsToGlobalBadge(projectId, skillId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/globalBadge/exists`)
      .then((response) => response.data);
  },
  getSkillsAvailableInCatalog(projectId, pagingParams = {}) {
    return axios.get(`/admin/projects/${projectId}/skills/catalog`, { params: { ...pagingParams } }).then((response) => response.data);
  },
  getSkillsExportedToCatalog(projectId, pagingParams = {}) {
    return axios.get(`/admin/projects/${projectId}/skills/exported`, { params: { ...pagingParams } }).then((response) => response.data);
  },
  getSkillsImportedFromCatalog(projectId, pagingParams = {}) {
    return axios.get(`/admin/projects/${projectId}/skills/imported`, { params: { ...pagingParams } }).then((response) => response.data);
  },
};

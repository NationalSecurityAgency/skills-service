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
import dayjs from '@/common-components/DayJsCustomizer'
import { useExportUtil } from '@/components/utils/UseExportUtil.js';

const exportUtil = useExportUtil()

const enrichSkillObjWithRequiredAtts = (skill) => {
  const copy = { ...skill }
  if (!skill.timeWindowEnabled) {
    copy.pointIncrementInterval = 0
  } else {
    // convert to minutes
    copy.pointIncrementInterval =
      parseInt(skill.pointIncrementIntervalHrs, 10) * 60 +
      parseInt(skill.pointIncrementIntervalMins, 10)
  }
  copy.numMaxOccurrencesIncrementInterval = skill.numPointIncrementMaxOccurrences

  return copy
}

export default {
  addMetaToSkillObj(skill) {
    const isCatalogImportedSkills = skill.copiedFromProjectId !== null && skill.copiedFromProjectId !== undefined && skill.copiedFromProjectId !== '';
    let catalogType = isCatalogImportedSkills ? 'imported' : null;
    if (skill.sharedToCatalog) {
      catalogType = 'exported';
    }
    const isCatalogSkill = isCatalogImportedSkills || skill.sharedToCatalog;

    return this.enhanceWithTimeWindow({
      ...skill,
      isGroupType: skill.type === 'SkillsGroup',
      isSkillType: skill.type === 'Skill',
      created: dayjs(skill.created),
      selfReportingType: (skill.type === 'Skill' && !skill.selfReportingType) ? 'Disabled' : skill.selfReportingType,
      isCatalogSkill,
      isCatalogImportedSkills,
      catalogType,
    });
  },
  getSkillDetails(projectId, subjectId, skillId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}/skills/${encodeURIComponent(skillId)}`
      )
      .then((response) => {
        const skill = response.data
        skill.subjectId = subjectId
        return this.addMetaToSkillObj(skill)
      })
  },
  getSkillInfo(projectId, skillId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}`)
      .then((response) => response.data)
  },
  enhanceWithTimeWindow(skill) {
    const copy = { ...skill }

    copy.timeWindowEnabled = skill.pointIncrementInterval > 0
    if (!copy.timeWindowEnabled) {
      // set to default if window is disabled
      copy.pointIncrementIntervalHrs = 8
      copy.pointIncrementIntervalMins = 0
    } else {
      copy.pointIncrementIntervalHrs = Math.floor(skill.pointIncrementInterval / 60)
      copy.pointIncrementIntervalMins = skill.pointIncrementInterval % 60
    }
    copy.numPointIncrementMaxOccurrences = skill.numMaxOccurrencesIncrementInterval

    return copy
  },
  getSubjectSkills(projectId, subjectId, includeGroupSkills = false) {
    const queryParam = includeGroupSkills ? '?includeGroupSkills=true' : ''
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}/skills${queryParam}`
      )
      .then((response) => response.data.map((item) => this.addMetaToSkillObj(item)))
  },
  exportSubjectSkills(projectId, subjectId) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}/skills/export/excel`;
    return exportUtil.ajaxDownload(url)
  },
  getProjectSkills(
    projectId,
    skillNameQuery = null,
    includeDisabled = true,
    excludeReusedSkills = false
  ) {
    let params = `?includeDisabled=${includeDisabled}`
    if (skillNameQuery) {
      params = `${params}&skillNameQuery=${encodeURIComponent(skillNameQuery)}`
    }
    if (excludeReusedSkills) {
      params = `${params}&excludeReusedSkills=${excludeReusedSkills}`
    }
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/skills${params}`)
      .then((response) => response.data)
  },
  getProjectSkillsWithoutImportedSkills(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/skills?excludeImportedSkills=true`)
      .then((response) => response.data)
  },
  getProjectSkillsAndBadgesWithoutImportedSkills(projectId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/skillsAndBadges?excludeImportedSkills=true`
      )
      .then((response) => response.data)
  },
  getProjectSkillsAndBadgesWithImportedSkills(projectId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/skillsAndBadges?excludeImportedSkills=false&excludeReusedSkills=true`
      )
      .then((response) => response.data)
  },
  getGroupSkills(projectId, groupId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/groups/${encodeURIComponent(groupId)}/skills`
      )
      .then((response) => response.data.map((item) => {
        item.groupId = groupId
        return this.addMetaToSkillObj(item)
      }))
  },
  saveSkill(skill) {
    const copy = enrichSkillObjWithRequiredAtts(skill)
    let requestSkillId = skill.skillId
    if (skill.isEdit) {
      requestSkillId = skill.originalSkillId
    }
    const url =
      skill.groupId && skill.groupId.length > 0
        ? `/admin/projects/${encodeURIComponent(skill.projectId)}/subjects/${encodeURIComponent(skill.subjectId)}/groups/${encodeURIComponent(skill.groupId)}/skills/${encodeURIComponent(requestSkillId)}`
        : `/admin/projects/${encodeURIComponent(skill.projectId)}/subjects/${encodeURIComponent(skill.subjectId)}/skills/${encodeURIComponent(requestSkillId)}`

    return axios
      .post(url, copy)
      .then(() => this.getSkillDetails(skill.projectId, skill.subjectId, skill.skillId))
  },
  reuseSkillInAnotherSubject(projectId, skillIds, newSubjectId, newGroupId = null) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/skills/reuse`
    return axios.post(url, {
      subjectId: newSubjectId,
      groupId: newGroupId,
      skillIds
    }, { handleError: false })
  },
  moveSkills(projectId, skillIds, newSubjectId, newGroupId = null, handleError = true) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/skills/move`
    return axios.post(
      url,
      {
        subjectId: newSubjectId,
        groupId: newGroupId,
        skillIds
      },
      { handleError }
    )
  },
  getReusedSkills(projectId, parentSkillId) {
    const url = `/admin/projects/${projectId}/reused/${parentSkillId}/skills`
    return axios.get(url).then((response) => response.data)
  },
  getReuseDestinationsForASkill(projectId, skillId) {
    const url = `/admin/projects/${projectId}/skills/${skillId}/reuse/destinations`
    return axios.get(url).then((response) => response.data)
  },
  checkSkillsForDeps(projectId, skillIds) {
    const url = `/admin/projects/${projectId}/hasDependency`
    return axios.post(url, { skillIds }).then((response) => response.data)
  },
  updateImportedSkill(skill) {
    const url = `/admin/projects/${encodeURIComponent(skill.projectId)}/import/skills/${encodeURIComponent(skill.skillId)}`
    return axios.patch(url, { pointIncrement: skill.pointIncrement }).then((res) => res.data)
  },
  deleteSkill(skill) {
    return axios
      .delete(
        `/admin/projects/${encodeURIComponent(skill.projectId)}/subjects/${encodeURIComponent(skill.subjectId)}/skills/${encodeURIComponent(skill.skillId)}`
      )
      .then((res) => res.data)
  },
  updateSkill(skill, actionToSubmit) {
    return axios
      .patch(
        `/admin/projects/${encodeURIComponent(skill.projectId)}/subjects/${encodeURIComponent(skill.subjectId)}/skills/${encodeURIComponent(skill.skillId)}`,
        {
          action: actionToSubmit
        }
      )
      .then(() => this.getSkillDetails(skill.projectId, skill.subjectId, skill.skillId))
  },
  getDependentSkillsGraphForProject(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/dependency/graph`)
      .then((res) => res.data)
  },
  assignSkillToBadge(projectId, badgeId, skillId) {
    return axios
      .post(
        `/admin/projects/${encodeURIComponent(projectId)}/badge/${encodeURIComponent(badgeId)}/skills/${encodeURIComponent(skillId)}`,
        null,
        { handleError: false }
      )
      .then((res) => res.data)
  },
  assignSkillsToBadge(projectId, badgeId, skillIds, handleError = true) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/badge/${encodeURIComponent(badgeId)}/skills/add`
    return axios.post(
      url,
      {
        skillIds
      },
      { handleError }
    )
  },
  removeSkillFromBadge(projectId, badgeId, skillId) {
    return axios
      .delete(
        `/admin/projects/${encodeURIComponent(projectId)}/badge/${encodeURIComponent(badgeId)}/skills/${encodeURIComponent(skillId)}`
      )
      .then((res) => res.data)
  },
  getBadgeSkills(projectId, badgeId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/badge/${encodeURIComponent(badgeId)}/skills`
      )
      .then((res) => res.data)
  },
  assignDependency(projectId, skillId, dependentSkillId, dependentProjectId) {
    return axios
      .post(
        `/admin/projects/${encodeURIComponent(projectId)}/${encodeURIComponent(skillId)}/prerequisite/${encodeURIComponent(dependentProjectId)}/${encodeURIComponent(dependentSkillId)}`
      )
      .then((createdRuleResult) => createdRuleResult.data)
  },
  validateDependency(projectId, skillId, dependentSkillId, dependentProjectId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/${encodeURIComponent(skillId)}/prerequisiteValidate/${encodeURIComponent(dependentProjectId)}/${encodeURIComponent(dependentSkillId)}`
      )
      .then((createdRuleResult) => createdRuleResult.data)
  },
  removeDependency(projectId, skillId, dependentSkillId, dependentProjectId) {
    return axios
      .delete(
        `/admin/projects/${encodeURIComponent(projectId)}/${encodeURIComponent(skillId)}/prerequisite/${encodeURIComponent(dependentProjectId)}/${encodeURIComponent(dependentSkillId)}`
      )
      .then((createdRuleResult) => createdRuleResult.data)
  },
  skillWithNameExists(projectId, skillName) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(projectId)}/skillNameExists`, { name: skillName })
      .then((remoteRes) => !remoteRes.data)
  },
  skillWithIdExists(projectId, skillId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/entityIdExists?id=${encodeURIComponent(skillId)}`
      )
      .then((remoteRes) => !remoteRes.data)
  },
  getLatestSkillVersion(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/latestVersion`)
      .then((remoteRes) => remoteRes.data)
  },
  saveSkillEvent(projectId, skillId, user, timestamp, doNotRequireApproval) {
    const userId = user.dn ? user.dn : user.userId
    return axios
      .put(
        `/api/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}`,
        { userId, timestamp, doNotRequireApproval },
        { handleError: false }
      )
      .then((remoteRes) => remoteRes.data)
  },
  checkIfSkillBelongsToGlobalBadge(projectId, skillId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/globalBadge/exists`
      )
      .then((response) => response.data)
  },
  getTagsForProject(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/skills/tags`)
      .then((response) => response.data)
  },
  getTagsForSkills(projectId, skillIds) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/skills/tags`
    return axios
      .post(url, {
        skillIds
      })
      .then((res) => res.data)
  },
  deleteTagForSkills(projectId, skillIds, tagId) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/skills/tag`
    return axios
      .delete(url, {
        data: {
          tagId,
          skillIds
        }
      })
      .then((res) => res.data)
  },
  addTagToSkills(projectId, skillIds, tagId, tagValue) {
    // const tagId = this.getTagIdFromValue(tagValue);
    const url = `/admin/projects/${encodeURIComponent(projectId)}/skills/tag`
    return axios
      .post(url, {
        tagId,
        tagValue,
        skillIds
      })
      .then((res) => res.data)
  }
  // getTagIdFromValue(tagValue) {
  //   return InputSanitizer.removeSpecialChars(tagValue)?.toLowerCase();
  // },
}

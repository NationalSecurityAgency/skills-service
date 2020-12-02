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

const findTopSkills = (sortedSkills, numInTopPercent) => {
  let minNum = sortedSkills[sortedSkills.length - numInTopPercent];
  let topSkills = sortedSkills.filter((s) => minNum <= s);
  const minimalSkillsAchieved = topSkills.length >= numInTopPercent && minNum > 0;
  if (topSkills.length > numInTopPercent) {
    topSkills = topSkills.filter((s) => s > minNum);
    if (topSkills.length > 0) {
      minNum = (topSkills.length > 0) ? topSkills[0] : null;
    }
  }
  return {
    enabled: minimalSkillsAchieved && topSkills.length > 0,
    minNum,
  };
};

const findOverlookedSkills = (sortedSkills, numInTopPercent) => {
  const numIn20Percent = Math.trunc(sortedSkills.length * 0.2);
  const zeros = sortedSkills.filter((s) => s === 0);

  let maxNum = sortedSkills[numInTopPercent - 1];
  let skills = sortedSkills.filter((s) => maxNum >= s);

  // overlooked skills have to be at a minimum half of the median count
  const halfMedianUserCount = Math.trunc(sortedSkills[Math.trunc(sortedSkills.length / 2)] / 2);
  skills = skills.filter((s) => s < halfMedianUserCount);
  maxNum = Math.max(...skills);

  const enabled = zeros.length <= numIn20Percent && skills.length <= numIn20Percent && skills.length > 0;
  return {
    enabled,
    maxNum,
  };
};

const findHighActivitySkills = (skills, numInTopPercent) => {
  const sortedSkills = skills.map((item) => item.numUsersInProgress).sort((a, b) => (a - b));
  let minNum = sortedSkills[sortedSkills.length - numInTopPercent];
  let topSkills = sortedSkills.filter((s) => minNum <= s);
  const minimalSkillsAchieved = topSkills.length >= numInTopPercent && minNum > 0;
  if (topSkills.length > numInTopPercent) {
    topSkills = topSkills.filter((s) => s > minNum);
    if (topSkills.length > 0) {
      minNum = (topSkills.length > 0) ? topSkills[0] : null;
    }
  }

  return {
    enabled: minimalSkillsAchieved && topSkills.length > 0,
    minNum,
  };
};

const objWithDisabledTags = (item) => ({
  isTopSkillTag: false,
  isNeverAchievedTag: !item.lastAchievedTimestamp,
  isNeverReportedTag: !item.lastReportedTimestamp,
  isOverlookedTag: false,
  isHighActivityTag: false,
  ...item,
});

export default {
  addTags(skills) {
    const numIn10Percent = Math.trunc(skills.length * 0.1);
    // up-to number of items in top or bottom skill tags
    const enabled = skills.length > 15;
    if (!enabled) {
      return skills.map((item) => objWithDisabledTags(item));
    }

    const sortedByNumAchieved = skills.map((item) => item.numUserAchieved).sort((a, b) => (a - b));
    const topSkill = findTopSkills(sortedByNumAchieved, numIn10Percent);
    const overlookedSkills = findOverlookedSkills(sortedByNumAchieved, numIn10Percent);
    const highActivitySkills = findHighActivitySkills(skills, numIn10Percent);

    return skills.map((item) => ({
      isTopSkillTag: (enabled && topSkill.enabled && topSkill.minNum <= item.numUserAchieved),
      isNeverAchievedTag: !item.lastAchievedTimestamp,
      isNeverReportedTag: !item.lastReportedTimestamp,
      isOverlookedTag: enabled && overlookedSkills.enabled && item.numUserAchieved <= overlookedSkills.maxNum,
      isHighActivityTag: enabled && highActivitySkills.enabled && (item.numUsersInProgress >= highActivitySkills.minNum),
      ...item,
    }));
  },
  shouldKeep(filters, item) {
    if (filters.name && !item.skillName.toLowerCase().includes(filters.name.toLowerCase())) {
      return false;
    }
    if (filters.neverAchieved && !item.isNeverAchievedTag) {
      return false;
    }
    if (filters.neverReported && !item.isNeverReportedTag) {
      return false;
    }
    if (filters.topSkillTag && !item.isTopSkillTag) {
      return false;
    }
    if (filters.overlookedTag && !item.isOverlookedTag) {
      return false;
    }
    if (filters.highActivityTag && !item.isHighActivityTag) {
      return false;
    }
    return true;
  },
};

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
// var moment = require('moment-timezone');
import SkillsUsageHelper from '@/components/metrics/skills/SkillsUsageHelper.js';

describe('SkillsUsageHelper', () => {
  it('empty for empty', () => {
    expect(SkillsUsageHelper.addTags([]))
      .toEqual([]);
  });

  it('enabled when there are more than 15 items', () => {
    const skills = [];
    const numSkills = 15;
    // const mForReported = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');
    // const mForAchieved = moment.utc('2020-08-12 11', 'YYYY-MM-DD HH');
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: i,
        numUsersInProgress: i,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }

    // lastReportedTimestamp: mForReported.clone().subtract(1, 'day').format('x'),
    //   lastAchievedTimestamp: mForAchieved.clone().subtract(1, 'day').format('x'),

    const res = SkillsUsageHelper.addTags(skills);
    expect(res.length).toEqual(numSkills);
    for (let i = 0; i < numSkills; i += 1) {
      expect(res[i].isTopSkillTag).toEqual(false);
      expect(res[i].isOverlookedTag).toEqual(false);
      expect(res[i].isHighActivityTag).toEqual(false);

      expect(res[i].isNeverAchievedTag).toEqual(true);
      expect(res[i].isNeverReportedTag).toEqual(true);
    }

    skills.push({
      skillId: 'skill16',
      skillName: 'Very Great Skill # 16',
      numUserAchieved: 25,
      numUsersInProgress: 25,
      lastReportedTimestamp: null,
      lastAchievedTimestamp: null,
    });

    const resAfter = SkillsUsageHelper.addTags(skills);
    const topSkills = resAfter.filter((item) => item.isTopSkillTag);
    expect(topSkills.length).toEqual(1);
    expect(topSkills[0].skillId).toEqual('skill16');
  });

  it('isTopSkillTag - top 10% is labeled with the tag', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: i,
        numUsersInProgress: i,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    skills.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skills);
    const topSkills = resAfter.filter((item) => item.isTopSkillTag);
    expect(topSkills.length).toEqual(5);
    expect(topSkills.map((i) => i.skillId).sort()).toEqual(['skill46', 'skill47', 'skill48', 'skill49', 'skill50']);
  });

  it('zero users achieved skills - isTopSkillTag, isOverlookedTag should not be assigned', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: 0,
        numUsersInProgress: i,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    skills.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skills);
    const topSkills = resAfter.filter((item) => item.isTopSkillTag);
    expect(topSkills.length).toEqual(0);

    const overlooked = resAfter.filter((item) => item.isOverlookedTag);
    expect(overlooked.length).toEqual(0);
  });

  it('zero users achieved skills - isHighActivityTag should not be assigned', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: i,
        numUsersInProgress: 0,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    skills.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skills);
    const highActivity = resAfter.filter((item) => item.isHighActivityTag);
    expect(highActivity.length).toEqual(0);
  });

  it('isTopSkillTag - at least 10 percent of skills have to have achievement counts before the tag is assigned', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: 0,
        numUsersInProgress: i,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    skills[23].numUserAchieved = 1;
    skills[18].numUserAchieved = 1;
    skills[5].numUserAchieved = 1;
    skills[30].numUserAchieved = 1;
    const skillsCopy1 = [...skills];
    skillsCopy1.sort(() => Math.random() - 0.5);
    const resBefore = SkillsUsageHelper.addTags(skillsCopy1);
    const topSkills = resBefore.filter((item) => item.isTopSkillTag);
    expect(topSkills.length).toEqual(0);

    skills[15].numUserAchieved = 1;
    const skillsCopy2 = [...skills];
    skillsCopy2.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skillsCopy2);
    const topSkillsAfter = resAfter.filter((item) => item.isTopSkillTag);
    expect(topSkillsAfter.length).toEqual(5);
    expect(topSkillsAfter.map((i) => i.skillId).sort()).toEqual(['skill16', 'skill19', 'skill24', 'skill31', 'skill6']);
  });

  it('isTopSkillTag - if the lowest number in the tag is shared by multiple skills then do not assign the tag if the number of those skills exceeds 10% of all the skills', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: 0,
        numUsersInProgress: i,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    skills[23].numUserAchieved = 1;
    skills[18].numUserAchieved = 1;
    skills[5].numUserAchieved = 1;
    skills[30].numUserAchieved = 1;
    skills[15].numUserAchieved = 1;
    skills[16].numUserAchieved = 1;

    const skillsCopy1 = [...skills];
    skillsCopy1.sort(() => Math.random() - 0.5);
    const resBefore = SkillsUsageHelper.addTags(skillsCopy1);
    const topSkills = resBefore.filter((item) => item.isTopSkillTag);
    expect(topSkills.length).toEqual(0);

    skills[16].numUserAchieved = 2;

    const skillsCopy2 = [...skills];
    skillsCopy2.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skillsCopy2);
    const topSkillsAfter = resAfter.filter((item) => item.isTopSkillTag);
    expect(topSkillsAfter.length).toEqual(1);
    expect(topSkillsAfter.map((i) => i.skillId).sort()).toEqual(['skill17']);

    skills[30].numUserAchieved = 2;

    const skillsCopy3 = [...skills];
    skillsCopy3.sort(() => Math.random() - 0.5);
    const resAfter3 = SkillsUsageHelper.addTags(skillsCopy3);
    const topSkillsAfter3 = resAfter3.filter((item) => item.isTopSkillTag);
    expect(topSkillsAfter3.length).toEqual(2);
    expect(topSkillsAfter3.map((i) => i.skillId).sort()).toEqual(['skill17', 'skill31']);
  });

  it('isOverlookedTag - zero users achieved skills - tag should not be assigned', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: 0,
        numUsersInProgress: i,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    skills.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skills);
    const taggedSkills = resAfter.filter((item) => item.isOverlookedTag);
    expect(taggedSkills.length).toEqual(0);
  });

  it('isOverlookedTag - bottom 10% is labeled with the tag', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: i,
        numUsersInProgress: i,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    skills.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skills);
    const taggedSkills = resAfter.filter((item) => item.isOverlookedTag);
    expect(taggedSkills.length).toEqual(5);
    expect(taggedSkills.map((i) => i.skillId).sort()).toEqual(['skill1', 'skill2', 'skill3', 'skill4', 'skill5']);
  });

  it('isOverlookedTag - at least 80 percent of skills have to have achievement counts before the tag is assigned, tag can go up to 20% for 0 counts', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: i <= 39 ? i : 0,
        numUsersInProgress: 0,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    const skillsCopy1 = [...skills];
    skillsCopy1.sort(() => Math.random() - 0.5);
    const resBefore = SkillsUsageHelper.addTags(skillsCopy1);
    const taggedSkills = resBefore.filter((item) => item.isOverlookedTag);
    expect(taggedSkills.length).toEqual(0);

    skills[49].numUserAchieved = 49;
    const skillsCopy2 = [...skills];
    skillsCopy2.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skillsCopy2);
    const taggedSkillsAfter = resAfter.filter((item) => item.isOverlookedTag);
    expect(taggedSkillsAfter.length).toEqual(10);
    expect(taggedSkillsAfter.map((i) => i.skillId).sort()).toEqual([
      'skill40',
      'skill41',
      'skill42',
      'skill43',
      'skill44',
      'skill45',
      'skill46',
      'skill47',
      'skill48',
      'skill49']);
  });

  it('isOverlookedTag - bottom 10% must be at least half of median number', () => {
    const skills = [];
    const numSkills = 16;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: (i <= 3) ? 4 : (i === 4 ? 5 : i),
        numUsersInProgress: i,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    const skillsCopy1 = [...skills];
    skillsCopy1.sort(() => Math.random() - 0.5);
    const resBefore = SkillsUsageHelper.addTags(skillsCopy1);
    const taggedSkills = resBefore.filter((item) => item.isOverlookedTag);
    expect(taggedSkills.length).toEqual(0);

    skills[2].numUserAchieved = 3;
    const skillsCopy2 = [...skills];
    skillsCopy2.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skillsCopy2);
    const taggedSkills1 = resAfter.filter((item) => item.isOverlookedTag);
    expect(taggedSkills1.length).toEqual(1);
    expect(taggedSkills1.map((i) => i.skillId).sort()).toEqual(['skill3']);
  });

  it('isHighActivityTag - top 10% is labeled with the tag', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: 0,
        numUsersInProgress: i * 2,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    const skillsCopy1 = [...skills];
    skillsCopy1.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skillsCopy1);
    const taggedSkills = resAfter.filter((item) => item.isHighActivityTag);
    expect(taggedSkills.length).toEqual(5);
    expect(taggedSkills.map((i) => i.skillId).sort()).toEqual(['skill46', 'skill47', 'skill48', 'skill49', 'skill50']);
  });

  it('isHighActivityTag - at least 10 percent of skills have to have achievement counts before the tag is assigned', () => {
    const skills = [];
    const numSkills = 50;
    for (let i = 1; i <= numSkills; i += 1) {
      skills.push({
        skillId: `skill${i}`,
        skillName: `Very Great Skill # ${i}`,
        numUserAchieved: i,
        numUsersInProgress: 0,
        lastReportedTimestamp: null,
        lastAchievedTimestamp: null,
      });
    }
    skills[23].numUsersInProgress = 10;
    skills[18].numUsersInProgress = 10;
    skills[5].numUsersInProgress = 10;
    skills[30].numUsersInProgress = 10;
    const skillsCopy1 = [...skills];
    skillsCopy1.sort(() => Math.random() - 0.5);
    const resBefore = SkillsUsageHelper.addTags(skillsCopy1);
    const topSkills = resBefore.filter((item) => item.isHighActivityTag);
    expect(topSkills.length)
      .toEqual(0);

    skills[15].numUsersInProgress = 10;
    const skillsCopy2 = [...skills];
    skillsCopy2.sort(() => Math.random() - 0.5);
    const resAfter = SkillsUsageHelper.addTags(skillsCopy2);
    const topSkillsAfter = resAfter.filter((item) => item.isHighActivityTag);
    expect(topSkillsAfter.length)
      .toEqual(5);
    expect(topSkillsAfter.map((i) => i.skillId)
      .sort())
      .toEqual(['skill16', 'skill19', 'skill24', 'skill31', 'skill6']);
  });

});

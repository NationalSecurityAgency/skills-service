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

export default {

    updateSkillPtsInList(skills, pts, skillId) {
        const index = skills.findIndex((item) => item.skillId === skillId);
        const skill = skills[index];
        const updatedSkill = this.addPts(skill, pts);
        skills.splice(index, 1, updatedSkill);
    },

    updateSkillPtsUnderSkillGroup(skills, pts, skillId, childSkillId) {
        const index = skills.findIndex((item) => item.skillId === skillId);
        const groupSkill = skills[index];
        this.updateSkillPtsInList(groupSkill.children, pts, childSkillId);

        let skillsToAddPtsFor = [];
        if (groupSkill.numSkillsRequired > 0) {
            const copyChildren = groupSkill.children.map((skill) => ({ ...skill }));
            copyChildren.sort((a, b) => b.points - a.points);
            skillsToAddPtsFor = copyChildren.slice(0, groupSkill.numSkillsRequired);
        } else {
            skillsToAddPtsFor = groupSkill.children;
        }

        const points = skillsToAddPtsFor.map((skill) => skill.points).reduce(
            (previousValue, currentValue) => previousValue + currentValue,
            0,
        );
        const todaysPoints = skillsToAddPtsFor.map((skill) => skill.todaysPoints).reduce(
            (previousValue, currentValue) => previousValue + currentValue,
            0,
        );
        const updatedSkill = ({ ...groupSkill, points, todaysPoints });
        skills.splice(index, 1, updatedSkill);
    },

    addPts(skill, pts) {
        const copy = { ...skill };
        copy.points += pts;
        copy.todaysPoints += pts;
        if (copy.points === copy.totalPoints) {
            copy.meta.complete = true;
            copy.achievedOn = new Date();
        }

        return copy;
    },

    addMeta(skill) {
        const copy = { ...skill };
        copy.meta = {
            complete: skill.points >= skill.totalPoints,
            selfReported: skill.selfReporting && skill.selfReporting.enabled === true,
            withPointsToday: skill.todaysPoints > 0,
            withoutProgress: skill.points === 0,
            inProgress: skill.points > 0 && skill.points < skill.totalPoints,
            pendingApproval: skill.selfReporting && skill.selfReporting.requestedOn && !skill.selfReporting.rejectedOn,
            belongsToBadge: skill.badges && skill.badges.length > 0,
            hasTag: skill.tags && skill.tags.length > 0,
            approval: skill.selfReporting && skill.selfReporting.type === 'Approval',
            honorSystem: skill.selfReporting && skill.selfReporting.type === 'HonorSystem',
            quiz: skill.selfReporting && skill.selfReporting.type === 'Quiz',
            survey: skill.selfReporting && skill.selfReporting.type === 'Survey',
        };
        return copy;
    },

    addMetaToSummary(summary) {
        const res = summary;
        res.skills = res.skills.map((item) => {
            const skillRes = this.addMeta(item);
            if (item.type === 'SkillsGroup' && item.children) {
                skillRes.children = skillRes.children.map((child) => this.addMeta(child));
                const numSkillsRequired = skillRes.numSkillsRequired === -1 ? skillRes.children.length : skillRes.numSkillsRequired;
                const numSkillsCompleted = skillRes.children.filter((childSkill) => childSkill.meta.complete).length;
                skillRes.meta.complete = numSkillsCompleted >= numSkillsRequired;
            }

            return skillRes;
        });
        return res;
    },

};

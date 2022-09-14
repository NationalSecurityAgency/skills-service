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
    updateSkillHistory(projectId, subjectId, skillId) {
        let seenSkills = JSON.parse(localStorage.getItem('lastSeenSkills'));
        if (!seenSkills) {
          seenSkills = {};
          seenSkills[projectId] = {};
          seenSkills[projectId][subjectId] = {
            lastSeenSkill: null,
          };
        }
        if (seenSkills[projectId]) {
          if (seenSkills[projectId][subjectId]) {
            seenSkills[projectId][subjectId].lastSeenSkill = skillId;
          }
        }
        localStorage.setItem('lastSeenSkills', JSON.stringify(seenSkills));
    },

    loadSkillHistory() {
        let lastSeen = JSON.parse(localStorage.getItem('lastSeenSkills'));

        if (!lastSeen) {
          lastSeen = {};
        }

        return lastSeen;
    },

};

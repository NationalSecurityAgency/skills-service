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
import { useStringUtils } from '@/common-components/utilities/UseStringUtils.js'

const stringUtils = useStringUtils();

export default {
  getTitle(skillItem, isCrossProject) {
    const container = document.createElement('div');

    // Treat all API values as text, including names in badge skill lists.
    const appendField = (label, value) => {
      const caption = document.createElement('span');
      caption.style.cssText = 'font-style: italic; color: #444444';
      caption.textContent = `${label}:`;
      container.append(caption, document.createTextNode(` ${value}`), document.createElement('br'));
    };
    if (isCrossProject) {
      const heading = document.createElement('span');
      heading.style.cssText = 'border-bottom: 1px dotted black; font-weight: bold;';
      const icon = document.createElement('i');
      icon.className = 'fas fa-handshake';
      heading.append(icon, document.createTextNode(' Cross Project Dependency'));
      const project = document.createElement('span');
      project.textContent = `Project ID: ${skillItem.projectId}`;
      container.append(heading, document.createElement('br'), project, document.createElement('br'));
    }
    appendField('Name', skillItem.name);
    appendField('ID', skillItem.skillId);
    if(skillItem.type === 'Skill') {
      appendField('Point Increment', skillItem.pointIncrement);
      appendField('Total Points', skillItem.totalPoints);
    }
    if(skillItem.type === 'Badge') {
      if(skillItem.containedSkills && skillItem.containedSkills.length > 0) {
        const skillNames = skillItem.containedSkills.map((it) => it.name);
        const caption = document.createElement('span');
        caption.style.cssText = 'font-style: italic; color: #444444';
        caption.textContent = 'Skills:';
        container.append(caption);
        const appendSkill = (name) => {
          const skill = document.createElement('span');
          skill.style.padding = '1rem';
          skill.textContent = name;
          container.append(document.createElement('br'), skill);
        };
        for (const [index, skillName] of skillNames.entries()) {
          appendSkill(skillName);
          if (index >= 9 && skillNames.length > 11) {
            // stop at 10 and truncate if there's more than 11
            appendSkill(`and ${skillNames.length-(index+1)} more skills...`);
            break
          }
        }
      }
    }
    return container;
  },
  getLabel(skillItem, isCrossProject) {
    return isCrossProject ? `Shared from\n<b>${this.truncate(skillItem.projectName)}</b>\n${this.truncate(skillItem.name)} ` : this.truncate(skillItem.name);
  },
  truncate(strValue, truncateTo = 20) {
    let chunks = strValue.split(' ');
    if(chunks.length > 1) {
      return stringUtils.addNewlinesToChunks(chunks, truncateTo)
    } else if (strValue.length > truncateTo) {
      return stringUtils.addNewlinesToString(strValue, truncateTo)
    }
    return strValue;
  },
};

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
import TruncateFilter from '../../../filters/TruncateFilter';

export default {
  getTitle(skillItem, isCrossProject) {
    let crossProjInfo = '';
    if (isCrossProject) {
      crossProjInfo = `<span style="border-bottom: 1px dotted black; font-weight: bold;"><i class="fas fa-handshake"></i> Cross Project Dependency</span><br/>
                           <span>Project ID: ${skillItem.projectId}</span><br/>`;
    }
    return `${crossProjInfo}<span style="font-style: italic; color: #444444">Name:</span> ${skillItem.name}<br/>
                <span style="font-style: italic; color: #444444">ID:</span> ${skillItem.skillId}<br/>
                <span style="font-style: italic; color: #444444">Point Increment:</span> ${skillItem.pointIncrement}<br/>
                <span style="font-style: italic; color: #444444">Total Points:</span> ${skillItem.totalPoints}`;
  },
  getLabel(skillItem, isCrossProject) {
    let res = isCrossProject ? `${this.truncate(skillItem.projectId, 10)} : ${skillItem.name} ` : skillItem.name;
    res = this.truncate(res);
    return res;
  },
  truncate(strValue, truncateTo = 35) {
    return TruncateFilter(strValue, truncateTo);
  },

};

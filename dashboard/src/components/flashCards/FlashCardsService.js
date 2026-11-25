/*
 * Copyright 2026 SkillTree
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
  getSkillFlashCards(projId, skillId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projId)}/skills/${encodeURIComponent(skillId)}/flashCards`)
      .then((response) => response.data);
  },
  saveSkillFlashCards(projId, skillId, flashCards) {
    return axios.post(`/admin/projects/${encodeURIComponent(projId)}/skills/${encodeURIComponent(skillId)}/flashCards`, flashCards, { handleError: false })
      .then((response) => response.data);
  },
};

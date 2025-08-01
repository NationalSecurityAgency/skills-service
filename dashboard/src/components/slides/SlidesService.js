/*
 * Copyright 2025 SkillTree
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
  saveSettings(container, item, slideSettings, isSkill = true) {
    const url = isSkill ? `/admin/projects/${container}/skills/${item}/slides` : `/admin/quiz-definitions/${container}/questions/${item}/slides`
    return axios.post(url, slideSettings)
      .then((response) => response.data);
  },
  getSettings(container, item, isSkill = true) {
    const url = isSkill ? `/admin/projects/${container}/skills/${item}/slides` : `/admin/quiz-definitions/${container}/questions/${item}/slides`
    return axios.get(url)
      .then((response) => response.data);
  },
  deleteSettings(container, item, isSkill = true) {
    const url = isSkill ? `/admin/projects/${container}/skills/${item}/slides` : `/admin/quiz-definitions/${container}/questions/${item}/slides`
    return axios.delete(url)
        .then((response) => response.data);
  },
};

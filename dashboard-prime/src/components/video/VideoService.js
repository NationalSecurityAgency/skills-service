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

export default {
  saveVideoSettings(projectId, skillId, videoSettings) {
    const url = `/admin/projects/${projectId}/skills/${skillId}/video`;
    return axios.post(url, videoSettings)
      .then((response) => response.data);
  },
  deleteVideoSettings(projectId, skillId) {
    const url = `/admin/projects/${projectId}/skills/${skillId}/video`;
    return axios.delete(url)
      .then((response) => response.data);
  },
  getVideoSettings(projectId, skillId) {
    const url = `/admin/projects/${projectId}/skills/${skillId}/video`;
    return axios.get(url)
      .then((response) => response.data);
  },
};

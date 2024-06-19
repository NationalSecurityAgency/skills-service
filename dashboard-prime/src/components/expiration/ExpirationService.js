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
  saveExpirationSettings(projectId, skillId, expirationSettings) {
    const url = `/admin/projects/${projectId}/skills/${skillId}/expiration`;
    return axios.post(url, expirationSettings)
      .then((response) => response.data);
  },
  deleteExpirationSettings(projectId, skillId, expirationSettings) {
    const url = `/admin/projects/${projectId}/skills/${skillId}/expiration`;
    return axios.delete(url, expirationSettings)
      .then((response) => response.data);
  },
  getExpirationSettings(projectId, skillId) {
    const url = `/admin/projects/${projectId}/skills/${skillId}/expiration`;
    return axios.get(url)
      .then((response) => response.data);
  },
  getExpiredSkills(projectId, params) {
    const url = `/admin/projects/${projectId}/expirations`;
    return axios.get(url, { params }).then((response) => response.data);
  },
};

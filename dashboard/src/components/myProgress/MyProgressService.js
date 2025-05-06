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
import axios from 'axios'

export default {
  loadMyProgressSummary() {
    return axios.get('/api/myProgressSummary').then((response) => response.data)
  },
  loadMyBadges() {
    return axios.get('/api/mybadges').then((response) => response.data)
  },
  findProjectName(projId) {
    return axios.get(`/api/myprojects/${encodeURIComponent(projId)}/name`).then((resp) => resp.data)
  },
  contactOwners(projId, msg) {
    return axios
      .post(`/api/projects/${encodeURIComponent(projId)}/contact`, { message: msg })
      .then((resp) => resp.data)
  },
  requestNewInvite(projId) {
    return axios
        .post(`/api/projects/${encodeURIComponent(projId)}/newInviteRequest`)
        .then((resp) => resp.data)
  }
}

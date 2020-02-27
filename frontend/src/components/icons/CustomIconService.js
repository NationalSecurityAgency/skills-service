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
  getCustomIconCss(projectId, isSupervisor) {
    let url;
    if (projectId) {
      url = `/api/projects/${projectId}/customIconCss`;
    } else if (isSupervisor) {
      url = '/api/icons/customIconCss';
    }
    if (url) {
      return axios
        .get(url)
        .then(response => response.data);
    }
    return new Promise(resolve => resolve(null));
  },
};

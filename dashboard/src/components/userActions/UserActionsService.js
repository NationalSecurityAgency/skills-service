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
  getDashboardActionsForEverything(params) {
    const url = '/root/dashboardActions';
    return axios.get(url, { params })
      .then((response) => response.data);
  },
  getDashboardActionsFilterOptions() {
    const url = '/root/dashboardActions/filterOptions';
    return axios.get(url)
      .then((response) => response.data);
  },
  getDashboardSingleAction(actionId) {
    const url = `/root/dashboardActions/${actionId}/attributes`;
    return axios.get(url)
      .then((response) => response.data);
  },
};

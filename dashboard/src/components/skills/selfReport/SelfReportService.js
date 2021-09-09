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
  getApprovals(projectId, params) {
    const url = `/admin/projects/${projectId}/approvals`;
    return axios.get(url, { params })
      .then((response) => response.data);
  },
  getApprovalsHistory(projectId, params) {
    const url = `/admin/projects/${projectId}/approvals/history`;
    return axios.get(url, { params })
      .then((response) => response.data);
  },
  approve(projectId, approvalIds) {
    const url = `/admin/projects/${projectId}/approvals/approve`;
    return axios.post(url, { skillApprovalIds: approvalIds })
      .then((response) => response.data);
  },
  reject(projectId, approvalIds, rejectMsg) {
    const url = `/admin/projects/${projectId}/approvals/reject`;
    return axios.post(url, { skillApprovalIds: approvalIds, rejectionMessage: rejectMsg })
      .then((response) => response.data);
  },
  getSelfReportStats(projectId) {
    const url = `/admin/projects/${projectId}/selfReport/stats`;
    return axios.get(url)
      .then((response) => response.data);
  },
  getSkillApprovalsStats(projectId, skillId) {
    const url = `/admin/projects/${projectId}/skills/${skillId}/approvals/stats`;
    return axios.get(url)
      .then((response) => response.data);
  },
  isEmailServiceSupported() {
    return axios.get('/public/isFeatureSupported?feature=emailservice').then((response) => response.data);
  },
};

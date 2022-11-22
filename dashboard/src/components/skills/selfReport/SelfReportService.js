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
    const url = `/admin/projects/${encodeURIComponent(projectId)}/approvals`;
    return axios.get(url, { params })
      .then((response) => response.data);
  },
  getApprovalsHistory(projectId, params) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/approvals/history`;
    return axios.get(url, { params })
      .then((response) => response.data);
  },
  approve(projectId, approvalIds) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/approvals/approve`;
    return axios.post(url, { skillApprovalIds: approvalIds })
      .then((response) => response.data);
  },
  reject(projectId, approvalIds, rejectMsg) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/approvals/reject`;
    return axios.post(url, { skillApprovalIds: approvalIds, rejectionMessage: rejectMsg })
      .then((response) => response.data);
  },
  getSelfReportStats(projectId) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/selfReport/stats`;
    return axios.get(url)
      .then((response) => response.data);
  },
  getSkillApprovalsStats(projectId, skillId) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/approvals/stats`;
    return axios.get(url)
      .then((response) => response.data);
  },
  isUserSubscribedToEmails(projectId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/approvalEmails/isSubscribed`).then((response) => response.data);
  },
  subscribeUserToEmails(projectId) {
    return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/approvalEmails/subscribe`).then((response) => response.data);
  },
  unsubscribeUserFromEmails(projectId) {
    return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/approvalEmails/unsubscribe`).then((response) => response.data);
  },
  getApproverConf(projectId) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/approverConf`;
    return axios.get(url)
      .then((response) => response.data);
  },
  configureApproverForUserTag(projectId, approverId, userTagKey, userTagValue) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/approverConf/${encodeURIComponent(approverId)}`;
    return axios.post(url, { userTagKey, userTagValue })
      .then((response) => response.data);
  },
  configureApproverForSkillId(projectId, approverId, skillId) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/approverConf/${encodeURIComponent(approverId)}`;
    return axios.post(url, { skillId })
      .then((response) => response.data);
  },
  configureApproverForUserId(projectId, approverId, userId) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/approverConf/${encodeURIComponent(approverId)}`;
    return axios.post(url, { userId })
      .then((response) => response.data);
  },
  configureApproverForFallback(projectId, approverId) {
    const url = `/admin/projects/${encodeURIComponent(projectId)}/approverConf/${encodeURIComponent(approverId)}/fallback`;
    return axios.post(url)
      .then((response) => response.data);
  },
  removeApproverConfig(projectId, approverConfigID) {
    return axios.delete(`/admin/projects/${encodeURIComponent(projectId)}/approverConf/${approverConfigID}`).then((response) => response.data);
  },
};

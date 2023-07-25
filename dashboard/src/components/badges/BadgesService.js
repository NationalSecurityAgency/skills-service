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

const enrichBadgeObjWithRequiredAtts = (badge) => {
  const copy = { ...badge };
  if (!badge.timeLimitEnabled) {
    copy.awardAttrs.numMinutes = 0;
  } else {
    // convert to minutes
    copy.awardAttrs.numMinutes = ((parseInt(badge.expirationHrs, 10) * 60) + parseInt(badge.expirationMins, 10));
  }

  return copy;
};
export default {
  enhanceWithTimeWindow(badge) {
    const copy = { ...badge };

    if (badge && badge.awardAttrs && badge.awardAttrs.numMinutes) {
      copy.timeLimitEnabled = badge.awardAttrs.numMinutes > 0;
      if (!copy.timeLimitEnabled) {
        // set to default if window is disabled
        copy.expirationHrs = 8;
        copy.expirationMins = 0;
      } else {
        copy.expirationHrs = Math.floor(badge.awardAttrs.numMinutes / 60);
        copy.expirationMins = badge.awardAttrs.numMinutes % 60;
      }
    } else {
      copy.timeLimitEnabled = false;
      copy.expirationHrs = 8;
      copy.expirationMins = 0;
    }

    return copy;
  },
  getBadges(projectId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/badges`)
      .then((response) => {
        const badges = [];
        response.data.forEach((badge) => {
          badges.push(this.enhanceWithTimeWindow(badge));
        });
        return badges;
      });
  },
  getBadge(projectId, badgeId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/badges/${encodeURIComponent(badgeId)}`)
      .then((response) => {
        const badge = response.data;
        return this.enhanceWithTimeWindow(badge);
      });
  },
  saveBadge(badgeReq) {
    const copy = enrichBadgeObjWithRequiredAtts(badgeReq);
    if (badgeReq.isEdit) {
      return axios.post(`/admin/projects/${encodeURIComponent(badgeReq.projectId)}/badges/${encodeURIComponent(badgeReq.originalBadgeId)}`, copy)
        .then(() => this.getBadge(badgeReq.projectId, badgeReq.badgeId));
    }

    const req = { enabled: false, ...badgeReq };
    return axios.post(`/admin/projects/${encodeURIComponent(req.projectId)}/badges/${encodeURIComponent(req.badgeId)}`, req)
      .then(() => this.getBadge(req.projectId, req.badgeId));
  },
  deleteBadge(projectId, badgeId) {
    return axios.delete(`/admin/projects/${encodeURIComponent(projectId)}/badges/${encodeURIComponent(badgeId)}`)
      .then((repsonse) => repsonse.data);
  },
  updateBadgeDisplaySortOrder(projectId, badgeId, newDisplayOrderIndex) {
    return axios.patch(`/admin/projects/${encodeURIComponent(projectId)}/badges/${encodeURIComponent(badgeId)}`, { action: 'NewDisplayOrderIndex', newDisplayOrderIndex });
  },
  badgeWithNameExists(projectId, badgeName) {
    return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/badgeNameExists`, { name: badgeName })
      .then((remoteRes) => !remoteRes.data);
  },
  badgeWithIdExists(projectId, badgeId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/entityIdExists?id=${encodeURIComponent(badgeId)}`)
      .then((remoteRes) => !remoteRes.data);
  },
};

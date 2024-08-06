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
  subjectWithNameExists(projectId, subjectName) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(projectId)}/subjectNameExists`, {
        name: subjectName
      })
      .then((remoteRes) => !remoteRes.data)
  },
  subjectWithIdExists(projectId, subjectId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/entityIdExists?id=${encodeURIComponent(subjectId)}`
      )
      .then((remoteRes) => !remoteRes.data)
  },
  getSubjectDetails(projectId, subjectId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}`
      )
      .then((res) => res.data)
  },
  getSubjects(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/subjects`)
      .then((res) => res.data)
  },
  saveSubject(subject) {
    if (subject.isEdit) {
      return axios
        .post(
          `/admin/projects/${encodeURIComponent(subject.projectId)}/subjects/${encodeURIComponent(subject.originalSubjectId)}`,
          subject
        )
        .then(() => this.getSubjectDetails(subject.projectId, subject.subjectId))
    }
    return axios
      .post(
        `/admin/projects/${encodeURIComponent(subject.projectId)}/subjects/${encodeURIComponent(subject.subjectId)}`,
        subject
      )
      .then(() => this.getSubjectDetails(subject.projectId, subject.subjectId))
  },
  updateSubjectsDisplaySortOrder(projectId, subjectId, newDisplayOrderIndex) {
    return axios.patch(
      `/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}`,
      { action: 'NewDisplayOrderIndex', newDisplayOrderIndex }
    )
  },
  deleteSubject(subject) {
    return axios.delete(
      `/admin/projects/${encodeURIComponent(subject.projectId)}/subjects/${encodeURIComponent(subject.subjectId)}`
    )
  },
  checkIfSubjectBelongsToGlobalBadge(projectId, subjectId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}/globalBadge/exists`
      )
      .then((response) => response.data)
  },
  shareSubject(projectId, subjectId, shareToProjectIds) {
    let data = {}
    if (shareToProjectIds) {
      data = { shareToProjectIds }
    }
    return axios
      .post(
        `/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}/share`,
        data
      )
      .then((res) => res.data)
  },
  unshareSubject(projectId, subjectId, unshareFromProjectIds) {
    let data = {}
    if (unshareFromProjectIds) {
      data = { unshareFromProjectIds }
    }
    return axios
      .post(
        `/admin/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(subjectId)}/unshare`,
        data
      )
      .then((res) => res.data)
  }
}

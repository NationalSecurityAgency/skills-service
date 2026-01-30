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
  getQuizDefs() {
    return axios.get('/app/quiz-definitions').then((response) => response.data)
  },
  updateQuizDef(quizDef) {
    const quizId = quizDef.originalQuizId ? quizDef.originalQuizId : quizDef.quizId
    const opType = quizDef.originalQuizId ? 'admin' : 'app'
    return axios
      .post(`/${opType}/quiz-definitions/${quizId}`, quizDef)
      .then((response) => response.data)
  },
  copyQuiz(quizDef) {
    const quizId = quizDef.originalQuizId;
    return axios.post(`/admin/quiz-definitions/${quizId}/copy`, quizDef, {handleError: false}).then((response) => response.data)
  },
  deleteQuizId(quizId) {
    return axios.delete(`/admin/quiz-definitions/${quizId}`).then((response) => response.data)
  },
  getQuizDef(quizId) {
    return axios.get(`/admin/quiz-definitions/${quizId}`).then((response) => response.data)
  },
  countNumSkillsQuizAssignedTo(quizId) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/skills-count`)
      .then((response) => response.data)
  },
  getQuizDefSummary(quizId) {
    return axios.get(`/admin/quiz-definitions/${quizId}/summary`).then((response) => response.data)
  },
  checkIfQuizIdExist(quizId) {
    return axios.post('/app/quizDefExist', { quizId }).then((response) => response.data)
  },
  checkIfQuizNameExist(name) {
    return axios.post('/app/quizDefExist', { name }).then((response) => response.data)
  },
  getQuizQuestionDefs(quizId) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/questions`)
      .then((response) => response.data)
  },
  getQuizQuestionDef(quizId, questionId) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/questions/${questionId}`)
      .then((response) => response.data)
  },
  saveQuizQuestionDef(quizId, newQuestionDef) {
    return axios
      .post(`/admin/quiz-definitions/${quizId}/create-question`, newQuestionDef)
      .then((response) => response.data)
  },
  updateQuizQuestionDef(quizId, existingQuestionDef) {
    return axios
      .post(
        `/admin/quiz-definitions/${quizId}/questions/${existingQuestionDef.id}`,
        existingQuestionDef
      )
      .then((response) => response.data)
  },
  updateQuizQuestionDisplaySortOrder(quizId, questionId, newDisplayOrderIndex) {
    return axios.patch(`/admin/quiz-definitions/${quizId}/questions/${questionId}`, {
      action: 'NewDisplayOrderIndex',
      newDisplayOrderIndex
    })
  },
  getQuizMetrics(quizId, params) {
    return axios.get(`/admin/quiz-definitions/${quizId}/metrics`, { params }).then((response) => response.data)
  },
  getQuizAnswerSelectionHistory(quizId, answerDefId, params) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/answers/${answerDefId}/attempts`, { params })
      .then((response) => response.data)
  },
  getQuizRunsHistory(quizId, params) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/runs`, { params })
      .then((response) => response.data)
  },
  getSingleQuizHistoryRun(quizId, attemptId) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/runs/${attemptId}`)
      .then((response) => response.data)
  },
  gradeQuizAnswerAttempt(quizId, userId, quizAttemptId, answerDefId, gradingInfo) {
    return axios
        .post(`/admin/quiz-definitions/${quizId}/users/${userId}/attempt/${quizAttemptId}/gradeAnswer/${answerDefId}`, gradingInfo)
        .then((response) => response.data)
  },
  deleteQuizRunHistoryItem(quizId, attemptId) {
    return axios
      .delete(`/admin/quiz-definitions/${quizId}/runs/${attemptId}`)
      .then((response) => response.data)
  },
  saveQuizSettings(quizId, settings) {
    return axios
      .post(`/admin/quiz-definitions/${quizId}/settings`, settings)
      .then((response) => response.data)
  },
  getQuizSettings(quizId) {
    return axios.get(`/admin/quiz-definitions/${quizId}/settings`).then((response) => response.data)
  },
  deleteQuizQuestion(quizId, questionId) {
    return axios
      .delete(`/admin/quiz-definitions/${quizId}/questions/${questionId}`)
      .then((response) => response.data)
  },
  getQuizUserRoles(quizId) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/userRoles`)
      .then((response) => response.data)
  },
  addQuizAdmin(quizId, userId) {
    const adminRole = 'ROLE_QUIZ_ADMIN'
    return axios.post(
        `/admin/quiz-definitions/${quizId}/users/${userId}/roles/${adminRole}`,
        null,
        { handleError: false }
      )
      .then((response) => response.data)
  },
  deleteQuizAdmin(quizId, userId) {
    const adminRole = 'ROLE_QUIZ_ADMIN'
    return axios
      .delete(`/admin/quiz-definitions/${quizId}/users/${userId}/roles/${adminRole}`)
      .then((response) => response.data)
  },
  getSkillsForQuiz(quizId) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/skills`)
      .then((response) => response.data)
  },
  getUserTagCounts(quizId, userTagKey, startDate, endDate) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/userTagCounts?userTagKey=${userTagKey}&startDate=${startDate}&endDate=${endDate}`)
      .then((response) => response.data)
  },
  getUsageOverTime(quizId, startDate, endDate) {
    return axios
      .get(`/admin/quiz-definitions/${quizId}/usageOverTime?startDate=${startDate}&endDate=${endDate}`)
      .then((response) => response.data)
  },
  saveMyPreference(quizId, preferenceKey, value) {
    return axios
        .post(`/admin/quiz-definitions/${quizId}/preferences/${preferenceKey}`, {value})
        .then((response) => response.data)
  },
  getMyPreferences(quizId) {
    return axios
        .get(`/admin/quiz-definitions/${quizId}/preferences`)
        .then((response) => response.data)
  },
  validateQuizForEnablingCommunity(quizId) {
    return axios
        .get(`/admin/quiz-definitions/${encodeURIComponent(quizId)}/validateEnablingCommunity`)
        .then((response) => response.data)
  },
  getTextInputAiGradingAttrs(quizId, questionId) {
    return axios
        .get(`/admin/quiz-definitions/${encodeURIComponent(quizId)}/questions/${questionId}/textInputAiGradingConf`)
        .then((response) => response.data)
  },
  saveTextInputAiGradingAttrs(quizId, questionId, attrs) {
    return axios
        .post(`/admin/quiz-definitions/${encodeURIComponent(quizId)}/questions/${questionId}/textInputAiGradingConf`, attrs)
        .then((response) => response.data)
  },
}

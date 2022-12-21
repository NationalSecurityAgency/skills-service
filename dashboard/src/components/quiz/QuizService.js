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
  getQuizDefs() {
    return axios.get('/app/quiz-definitions')
      .then((response) => response.data);
  },
  updateQuizDef(quizDef) {
    const quizId = quizDef.originalQuizId ? quizDef.originalQuizId : quizDef.quizId;
    const opType = quizDef.originalQuizId ? 'admin' : 'app';
    return axios.post(`/${opType}/quiz-definitions/${quizId}`, quizDef)
      .then((response) => response.data);
  },
  deleteQuizId(quizId) {
    return axios.delete(`/admin/quiz-definitions/${quizId}`)
      .then((response) => response.data);
  },
  getQuizDef(quizId) {
    return axios.get(`/admin/quiz-definitions/${quizId}`)
      .then((response) => response.data);
  },
  checkIfQuizIdExist(quizId) {
    return axios.post('/app/quizDefExist', { quizId })
      .then((response) => response.data);
  },
  checkIfQuizNameExist(name) {
    return axios.post('/app/quizDefExist', { name })
      .then((response) => response.data);
  },
  getQuizQuestionDefs(quizId) {
    return axios.get(`/admin/quiz-definitions/${quizId}/questions`)
      .then((response) => response.data);
  },
  saveQuizQuestionDef(quizId, newQuestionDef) {
    return axios.post(`/admin/quiz-definitions/${quizId}/questions/create`, newQuestionDef)
      .then((response) => response.data);
  },
  updateQuizQuestionDisplaySortOrder(quizId, questionId, newDisplayOrderIndex) {
    return axios.patch(`/admin/quiz-definitions/${quizId}/questions/${questionId}`, {
      action: 'NewDisplayOrderIndex',
      newDisplayOrderIndex,
    });
  },
};

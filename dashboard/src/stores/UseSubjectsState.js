/*
 * Copyright 2024 SkillTree
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
import { ref } from 'vue'
import { defineStore } from 'pinia'
import SubjectsService from '@/components/subjects/SubjectsService.js'
import { useRoute } from 'vue-router'

export const useSubjectsState = defineStore('subjectsState', () => {
  const subject = ref({})
  const isLoadingSubject = ref(false)

  const route = useRoute()

  function loadSubjectDetailsState() {
    isLoadingSubject.value = true
    return new Promise((resolve, reject) => {
      SubjectsService.getSubjectDetails(route.params.projectId, route.params.subjectId)
        .then((response) => {
          subject.value = response
          resolve(response)
        })
        .catch((error) => reject(error))
        .finally(() => {
          isLoadingSubject.value = false
        })
    })
  }
  function setSubject(subject){
    subject.value = subject
  }

  const subjects = ref([])
  const isLoadingSubjects = ref(false)

  function loadSubjects(updateLoading = false) {
    if (updateLoading) {
      isLoadingSubjects.value = true
    }
    return SubjectsService.getSubjects(route.params.projectId)
      .then((response) => {
        subjects.value = response
      })
      .finally(() => {
        isLoadingSubjects.value = false
      })
  }

  function setSubjects(subjects) {
    subjects.value = subjects
  }

  return {
    subject,
    loadSubjectDetailsState,
    isLoadingSubject,
    setSubject,
    subjects,
    loadSubjects,
    isLoadingSubjects,
    setSubjects
  }

})
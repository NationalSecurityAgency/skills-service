import { ref } from 'vue'
import { defineStore } from 'pinia'
import SubjectsService from '@/components/subjects/SubjectsService.js'

export const useSubjectsState = defineStore('subjectsState', () => {
  const subject = ref({})
  const isLoadingSubject = ref(false)

  function loadSubjectDetailsState(payload) {
    isLoadingSubject.value = true
    return new Promise((resolve, reject) => {
      SubjectsService.getSubjectDetails(payload.projectId, payload.subjectId)
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

  function loadSubjects(payload) {
    isLoadingSubjects.value = true
    return new Promise((resolve, reject) => {
        SubjectsService.getSubjects(payload.projectId)
          .then((response) => {
            subjects.value = response
            resolve(response)
          })
          .catch((error) => reject(error))
          .finally(() => {
            isLoadingSubjects.value = false
          })
      }
    )
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
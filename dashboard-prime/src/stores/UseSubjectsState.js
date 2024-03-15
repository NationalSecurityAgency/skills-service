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
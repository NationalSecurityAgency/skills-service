import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRoute } from 'vue-router'
import SkillsService from '@/components/skills/SkillsService.js'
import dayjs from '@/common-components/DayJsCustomizer'

export const useSubjectSkillsState = defineStore('subjectSkillsState', () => {
  const subjectSkills = ref([])
  const loadingSubjectSkills = ref(false)
  const route = useRoute()

  function setSubjectSkills(value) {
    subjectSkills.value = value
  }

  function setLoadingSubjectSkills(value) {
    loadingSubjectSkills.value = value
  }

  const hasSkills = computed(() => {
    return !loadingSubjectSkills.value && subjectSkills.value.length > 0
  })

  function loadSubjectSkills(projectId, subjectId) {
    setLoadingSubjectSkills(true)
    return new Promise((resolve, reject) => {
      SkillsService.getSubjectSkills(projectId, subjectId)
        .then((loadedSkills) => {
          const updatedSkills = loadedSkills.map((loadedSkill) => ({
            ...loadedSkill,
            created: dayjs(loadedSkill.created),
            subjectId: route.params.subjectId,
            isGroupType: loadedSkill.type === 'SkillsGroup',
            isSkillType: loadedSkill.type === 'Skill',
            selfReportingType: (loadedSkill.type === 'Skill' && !loadedSkill.selfReportingType) ? 'Disabled' : loadedSkill.selfReportingType,
          }))
          setSubjectSkills(updatedSkills)
          resolve(updatedSkills)
        })
        .catch((error) => reject(error))
        .finally(() => {
          setLoadingSubjectSkills(false)
        })
    })
  }

  return {
    subjectSkills,
    loadingSubjectSkills,
    loadSubjectSkills,
    hasSkills
  }

})
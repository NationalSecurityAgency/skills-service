import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRoute } from 'vue-router'
import SkillsService from '@/components/skills/SkillsService.js'
import { useLog } from '@/components/utils/misc/useLog.js'

export const useSubjectSkillsState = defineStore('subjectSkillsState', () => {
  const subjectSkills = ref([])
  const groupSkills = ref(new Map())
  const loadingGroupSkills = ref(new Map())
  const loadingSubjectSkills = ref(false)
  const route = useRoute()
  const log = useLog()

  function setSubjectSkills(value) {
    log.trace('called setSubjectSkills')
    subjectSkills.value = value
  }

  function pushIntoSubjectSkills(itemsToAdd) {
    log.trace(`pushed ${itemsToAdd.length} into subjectSkills`)
    if (itemsToAdd instanceof Array) {
      subjectSkills.value.push(...itemsToAdd)
    } else {
      subjectSkills.value.push(itemsToAdd)
    }
  }

  const removeSubjectSkillsBySkillIds = (skillIdsToRemove) => {
    log.trace(`removing ${skillIdsToRemove.length} skills`)
    subjectSkills.value = subjectSkills.value.filter((skill) => !skillIdsToRemove.includes(skill.skillId))
  }

  function setLoadingSubjectSkills(value) {
    loadingSubjectSkills.value = value
  }

  function setLoadingGroupSkills(groupId, value) {
    loadingGroupSkills.value.set(groupId, value)
  }

  function getLoadingGroupSkills(groupId) {
    const res = loadingGroupSkills.value.get(groupId)
    return res || false
  }

  const hasSkills = computed(() => {
    return subjectSkills.value.length > 0
  })

  function loadSubjectSkills(projectId, subjectId, updateLoadingFlag = true) {
    log.trace(`loading subject skills for project=[${projectId}], subject=[${subjectId}], updateLoadingFlag=[${updateLoadingFlag}]`)
    if (updateLoadingFlag) {
      setLoadingSubjectSkills(true)
    }
    return SkillsService.getSubjectSkills(projectId, subjectId)
      .then((loadedSkills) => {
        const updatedSkills = loadedSkills.map((loadedSkill) => ({
          ...loadedSkill,
          subjectId: route.params.subjectId
        }))
        setSubjectSkills(updatedSkills)
      })
      .finally(() => {
        if (updateLoadingFlag) {
          setLoadingSubjectSkills(false)
        }
      })
  }

  const totalNumSkillsInSubject = computed(() => {
    const initValue = 0;
    const currentSkills= subjectSkills.value || [];
    const totalCurrentSkills = currentSkills.reduce((previousValue, currentValue) => {
      if (currentValue.numSkillsInGroup !== null) {
        return previousValue + currentValue.numSkillsInGroup;
      }
      return previousValue + 1;
    }, initValue);
    return totalCurrentSkills;
  })

  function setGroupSkills(groupId, value) {
    groupSkills.value.set(groupId, value)
  }
  function getGroupSkills(groupId) {
    return groupSkills.value.get(groupId) || []
  }

  function loadGroupSkills(projectId, groupId) {
    setLoadingGroupSkills(groupId, true)
    return SkillsService.getGroupSkills(projectId, groupId)
      .then((loadedSkills) => {
        const updatedSkills = loadedSkills.map((loadedSkill) => ({
          ...loadedSkill,
          subjectId: route.params.subjectId
        }))
        setGroupSkills(groupId, updatedSkills)
        const foundGroup = subjectSkills.value.find((skill) => skill.skillId === groupId)
        if(foundGroup) {
          foundGroup.numSkillsInGroup = updatedSkills.length
          foundGroup.totalPoints = updatedSkills.map((item) => item.totalPoints).reduce((accumulator, currentValue) => accumulator + currentValue, 0)
        }
      }).finally(() => {
        setLoadingGroupSkills(groupId, false)
      })
  }


  return {
    subjectSkills,
    setSubjectSkills,
    pushIntoSubjectSkills,
    removeSubjectSkillsBySkillIds,
    loadingSubjectSkills,
    setLoadingSubjectSkills,
    loadSubjectSkills,
    hasSkills,
    totalNumSkillsInSubject,
    setGroupSkills,
    getGroupSkills,
    loadGroupSkills,
    groupSkills,
    setLoadingGroupSkills,
    getLoadingGroupSkills
  }

})
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import SkillsService from '@/components/skills/SkillsService'

export const useSkillsState = defineStore('skillsState',  () => {
  const skillState = ref(null);
  const loadingSkill = ref(true);

  function loadSkill(projectId, subjectId, skillId) {
    loadingSkill.value = true;
    return new Promise((resolve, reject) => {
      SkillsService.getSkillDetails(projectId, subjectId, skillId).then((response) => {
        skillState.value = Object.assign(response, { subjectId });
        resolve(response)
      }).catch((error) => reject(error)).finally(() => {
        loadingSkill.value = false;
      })
    })
  }
  const skill = computed(() => skillState.value)
  const setSkill = (skill) => {
    skillState.value = skill
  }

  return { skill, setSkill, loadingSkill, loadSkill };

})
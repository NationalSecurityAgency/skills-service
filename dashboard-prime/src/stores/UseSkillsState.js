import { ref } from 'vue'
import { defineStore } from 'pinia'
import SkillsService from '@/components/skills/SkillsService'

export const useSkillsState = defineStore('skillsState',  () => {
  const skill = ref(null);
  const loadingSkill = ref(true);

  function loadSkill(projectId, subjectId, skillId) {
    loadingSkill.value = true;
    return new Promise((resolve, reject) => {
      SkillsService.getSkillDetails(projectId, subjectId, skillId).then((response) => {
        skill.value = Object.assign(response, { subjectId });
        resolve(response)
      }).catch((error) => reject(error)).finally(() => {
        loadingSkill.value = false;
      })
    })
  }

  return { skill, loadingSkill, loadSkill };

})
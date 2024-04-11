import { ref } from 'vue'
import axios from 'axios'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { defineStore } from 'pinia'

export const useUserProgressSummaryState = defineStore('userProgressSummaryState', () => {

  const servicePath = '/api/projects'
  const attributes = useSkillsDisplayAttributesState()

  const loadingUserProgressSummary = ref(true)
  const userProgressSummary = ref({})
  const getUserIdAndVersionParams = () => {
    // const params = this.getUserIdParams();
    // params.version = this.version;
    //
    // return params;
    return {}
  }
  const loadUserProgressSummary = () => {
    loadingUserProgressSummary.value = true
    return axios.get(`${attributes.serviceUrl}${servicePath}/${encodeURIComponent(attributes.projectId)}/summary`, {
      params: getUserIdAndVersionParams()
    }).then((result) => {
      userProgressSummary.value = result.data
    }).finally(() => {
      loadingUserProgressSummary.value = false
    })
  }

  return {
    userProgressSummary,
    loadUserProgressSummary,
    loadingUserProgressSummary,
  }

})
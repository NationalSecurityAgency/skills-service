import { ref, computed } from 'vue'
import axios from 'axios'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { defineStore } from 'pinia'

export const useUserProgressSummaryState = defineStore('userProgressSummaryState', () => {

  const servicePath = '/api/projects'
  const attributes = useSkillsDisplayAttributesState()

  const loadingUserProgressSummary = ref(true)
  const userProgressSummary = ref({})
  const loadingUserSkillsRanking = ref(true)
  const userRanking = ref({})

  const getUserIdAndVersionParams = () => {
    // const params = this.getUserIdParams();
    // params.version = this.version;
    //
    // return params;
    return {}
  }
  const loadUserProgressSummary = () => {
    console.log(`loadUserProgressSummary for ${attributes.projectId}`)
    return axios.get(`${attributes.serviceUrl}${servicePath}/${encodeURIComponent(attributes.projectId)}/summary`, {
      params: getUserIdAndVersionParams()
    }).then((result) => {
      userProgressSummary.value = result.data
    }).finally(() => {
      loadingUserProgressSummary.value = false
    })
  }

  const loadUserSkillsRanking = (subjectId) => {
    loadingUserSkillsRanking.value = true
    let url = `${attributes.serviceUrl}${servicePath}/${encodeURIComponent(attributes.projectId)}/subjects/${encodeURIComponent(subjectId)}/rank`
    if (!subjectId) {
      url = `${attributes.serviceUrl}${servicePath}/${encodeURIComponent(attributes.projectId)}/rank`
    }
    return axios.get(url, {
      params: getUserIdAndVersionParams()
    }).then((result) => {
      userRanking.value = result.data
    }).finally(() => {
      loadingUserSkillsRanking.value = false
    })
  }

  // const isLoadingData = computed(() => loadingUserProgressSummary.value || loadingUserSkillsRanking.value)
  // const loadData = () => {
  //   loadUserProgressSummary()
  //   loadUserSkillsRanking()
  // }
  return {
    userProgressSummary,
    loadUserProgressSummary,
    loadingUserProgressSummary,

    userRanking,
    loadUserSkillsRanking,
    loadingUserSkillsRanking,
  }

})
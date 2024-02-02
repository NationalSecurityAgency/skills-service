import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useClientDisplayPath = defineStore('clientDisplayPathState',  () => {
  const clientPathInfo = ref({ path: '/', fromDashboard: false })

  function setClientPathInfo(pathInfo) {
    clientPathInfo.value = pathInfo
  }

  return { clientPathInfo, setClientPathInfo };
})
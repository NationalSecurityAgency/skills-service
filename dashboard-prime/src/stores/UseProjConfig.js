import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import SettingService from '@/components/settings/SettingsService'
import UserRolesUtil from '@/components/utils/UserRolesUtil.js'

export const useProjConfig = defineStore('projConfig', () => {
  const projConfig = ref(null)
  const loadingProjConfig = ref(true)

  function loadProjConfigState(params) {
    const { projectId } = params
    const { updateLoadingVar = true } = params
    return new Promise((resolve, reject) => {
      if (updateLoadingVar) {
        loadingProjConfig.value = true
      }
      SettingService.getSettingsForProject(projectId)
        .then((response) => {
          if (response && typeof response.reduce === 'function') {
            const newProjConfig = response.reduce((map, obj) => {
              // eslint-disable-next-line no-param-reassign
              map[obj.setting] = obj.value
              return map
            }, {})
            projConfig.value = newProjConfig
          }
          resolve(response)
        })
        .finally(() => {
          if (updateLoadingVar) {
            loadingProjConfig.value = false
          }
        })
        .catch((error) => reject(error))
    })
  }

  function afterProjConfigStateLoaded() {
    return new Promise((resolve) => {
      (function waitForProjConfig() {
        if (!loadingProjConfig.value) return resolve(projConfig.value)
        setTimeout(waitForProjConfig, 100)
        return projConfig.value
      }())
    })
  }

  function getProjectCommunityValue() {
    return projConfig.value?.project_community_value
  }

  const isProjConfigInviteOnly = computed(() => {
    return projConfig.value && projConfig.value.invite_only === 'true';
  });

  const isProjConfigDiscoverable = computed(() => {
    return projConfig.value && projConfig.value['production.mode.enabled'] === 'true';
  });

  const projConfigRootHelpUrl = computed(() => {
    return projConfig.value && projConfig.value['help.url.root'];
  });

  const isReadOnlyProj = computed(() => {
    return projConfig.value && UserRolesUtil.isReadOnlyProjRole(projConfig.value.user_project_role);
  });

  const userProjRole = computed(() => {
    return projConfig.value && projConfig.value.user_project_role;
  });

  return {
    projConfig,
    loadingProjConfig,
    loadProjConfigState,
    afterProjConfigStateLoaded,
    getProjectCommunityValue,
    isProjConfigInviteOnly,
    isProjConfigDiscoverable,
    projConfigRootHelpUrl,
    isReadOnlyProj,
    userProjRole
  }
})
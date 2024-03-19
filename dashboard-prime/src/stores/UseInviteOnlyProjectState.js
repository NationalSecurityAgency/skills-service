import { ref } from 'vue'
import { defineStore } from 'pinia'
import SettingsService from '@/components/settings/SettingsService.js'

export const useInviteOnlyProjectState = defineStore('inviteOnlyProjectState', () => {
  const isInviteOnlyProject = ref(false)
  const loadInviteOnlySetting = (projectId) => {
    SettingsService.getProjectSetting(projectId, 'invite_only')
      .then((setting) => {
        isInviteOnlyProject.value = Boolean(setting?.enabled);
      });
  }

  return {
    isInviteOnlyProject,
    loadInviteOnlySetting
  }
})
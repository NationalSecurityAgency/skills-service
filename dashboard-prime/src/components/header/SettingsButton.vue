<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthState } from '@/stores/UseAuthState.js'
import { usePagePath } from '@/components/utils/UsePageLocation'
import { useUserInfo } from '@/components/utils/UseUserInfo'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const authState = useAuthState()
const userInfo = useUserInfo()
const displayName = computed(() => {
  const userInfoObj = userInfo.userInfo.value
  let displayName = userInfoObj.nickname
  if (!displayName) {
    displayName = `${userInfoObj.first} ${userInfoObj.last}`
  }
  return displayName
})

const router = useRouter()
const pagePath = usePagePath()

const menu = ref()
let allItems = [
  {
    separator: true
  }
]
const appConfig = useAppConfig()
if (appConfig.rankingAndProgressViewsEnabled) {
  allItems.push({
    label: 'Progress and Ranking',
    icon: 'fas fa-chart-bar',
    command: () => {
      router.push({ path: pagePath.progressAndRankingHomePage })
    },
    disabled: pagePath.isProgressAndRankingPage
  })
}

allItems.push({
  label: 'Project Admin',
  icon: 'fas fa-user-edit',
  command: () => {
    router.push({ path: pagePath.adminHomePage })
  },
  disabled: pagePath.isAdminPage
})
allItems.push({
  label: 'Settings',
  icon: 'fas fa-cog',
  command: () => {
    router.push({ path: pagePath.settingsHomePage })
  },
  disabled: pagePath.isSettingsPage
})

if (userInfo.isFormAuthenticatedUser.value) {
  allItems.push({
    separator: true
  })
  allItems.push({
    label: 'Log Out',
    icon: 'fas fa-sign-out-alt',
    command: () => {
      authState.logout()
    }
  })
}

const items = ref(allItems)

const toggle = (event) => {
  menu.value.toggle(event)
}
</script>

<template>
  <div class="d-inline">
    <Button
      icon="fas fa-user"
      severity="info"
      outlined
      raised
      @click="toggle"
      aria-label="User Settings Button"
      aria-haspopup="true"
      aria-controls="user_settings_menu" />
    <div id="user_settings_menu">
      <Menu ref="menu" :model="items" :popup="true">
        <template #start>
          <div class="mx-3 mt-2">
            <Avatar icon="fas fa-user" class="bg-lime-900 text-white" />
            <span data-cy="settingsButton-loggedInName" class="ml-1">{{ displayName }}</span>
          </div>
        </template>
      </Menu>
    </div>
  </div>
</template>

<style scoped></style>

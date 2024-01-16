<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { usePagePath } from '@/components/utils/UsePageLocation'
import { useUserInfo } from '@/components/utils/UseUserInfo'
import { useAppConfig } from '@/components/utils/UseAppConfig'

const store = useStore()
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
    icon: 'pi pi-chart-bar',
    command: () => {
      router.push({ path: pagePath.progressAndRankingHomePage })
    },
    disabled: pagePath.isProgressAndRankingPage
  })
}

allItems.push({
  label: 'Project Admin',
  icon: 'pi pi-user-edit',
  command: () => {
    router.push({ path: pagePath.adminHomePage })
  },
  disabled: pagePath.isAdminPage
})
allItems.push({
  label: 'Settings',
  icon: 'pi pi-cog',
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
    icon: 'pi pi-sign-out',
    command: () => {
      store.dispatch('logout')
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
      icon="pi pi-user"
      severity="info"
      rounded
      outlined
      raised
      @click="toggle"
      aria-label="User Settings Button"
      aria-haspopup="true"
      aria-controls="user_settings_menu" />
    <Menu ref="menu" id="user_settings_menu" :model="items" :popup="true">
      <template #start>
        <div class="mx-3 mt-2">
          <Avatar icon="pi pi-user" class="bg-info text-white" />
          <span data-cy="settingsButton-loggedInName" class="ms-1">{{ displayName }}</span>
        </div>
      </template>
    </Menu>
  </div>
</template>

<style scoped></style>

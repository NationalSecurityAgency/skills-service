/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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
if (appConfig.rankingAndProgressViewsEnabled && userInfo.userInfo.value.adminDashboardAccess) {
  allItems.push({
    label: 'Progress and Ranking',
    icon: 'fas fa-chart-bar',
    command: () => {
      router.push({ path: pagePath.progressAndRankingHomePage })
    },
    disabled: pagePath.isProgressAndRankingPage
  })
  allItems.push({
    label: 'Project Admin',
    icon: 'fas fa-user-edit',
    command: () => {
      router.push({ path: pagePath.adminHomePage })
    },
    disabled: pagePath.isAdminPage
  })
}
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
      <Menu ref="menu" :model="items" :popup="true" role="navigation">
        <template #start>
          <div class="px-3 py-3">
            <span class="mr-2">
              <Avatar icon="fas fa-user" :pt="{icon: {class: 'text-blue-800 dark:text-blue-400'}}" />
            </span>
            <span data-cy="settingsButton-loggedInName" class="ml-1">{{ displayName }}</span>
          </div>
        </template>
        <template #item="{ item, props }">
          <a :href="item.url" target="_blank" v-bind="props.action">
            <span class="w-7 border text-center rounded text-green-800 bg-green-50 dark:bg-gray-900 dark:text-green-500 dark:border-green-700"><i :class="item.icon"/></span>
            <span class="">{{ item.label }}</span>
          </a>
        </template>
      </Menu>
    </div>
  </div>
</template>

<style scoped></style>

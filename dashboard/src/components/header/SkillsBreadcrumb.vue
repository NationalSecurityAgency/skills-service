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
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useClientDisplayPath } from '@/stores/UseClientDisplayPath.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import SettingsService from '@/components/settings/SettingsService.js'
import SkillsBreadcrumbItem from '@/components/header/SkillsBreadcrumbItem.vue'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

const route = useRoute()
const clientDisplayPath = useClientDisplayPath()
const appConfig = useAppConfig()
const responsive = useResponsiveBreakpoints()
const items = ref([])
const smallScreenMode = computed(() => responsive.sm.value)
const skillsDisplayInfo = useSkillsDisplayInfo()

const idsToExcludeFromPath = ['subjects', 'skills', 'projects', 'crossProject', 'dependency', 'global']
const keysToExcludeFromPath = ['questions']

const build = () => {
  handleCustomLabels().then(() => {
    buildBreadcrumb()
  })
}
onMounted(() => {
  build()
})
watch(
  () => route.path,
  async () => {
    build()
  }
)

const projectDisplayName = ref('Project')
const subjectDisplayName = ref('Subject')
const groupDisplayName = ref('Group')
const skillDisplayName = ref('Skill')
const currentProjectId = ref('')
const handleCustomLabels = () => {
  return new Promise((resolve) => {
    const isProgressAndRankingsProject = skillsDisplayInfo.isSkillsDisplayPath()
    if (isProgressAndRankingsProject) {
      const currentProjectIdParam = route.params.projectId
      if (currentProjectId.value !== currentProjectIdParam) {
        SettingsService.getClientDisplayConfig(currentProjectIdParam).then((response) => {
          projectDisplayName.value = response.projectDisplayName
          subjectDisplayName.value = response.subjectDisplayName
          groupDisplayName.value = response.groupDisplayName
          skillDisplayName.value = response.skillDisplayName
          currentProjectId.value = currentProjectIdParam
          resolve()
        })
      } else {
        resolve()
      }
    } else {
      projectDisplayName.value = 'Project'
      subjectDisplayName.value = 'Subject'
      groupDisplayName.value = 'Group'
      skillDisplayName.value = 'Skill'
      currentProjectId.value = ''
      resolve()
    }
  })
}
const projectAndRankingPathItem = 'progress-and-rankings';
const adminHomePageItem = 'administrator'
const adminHomePageName = 'Projects'
const settingsHomePageItem = 'settings'
const buildPathInfo = () => {
  return {
    path: route.path,
    pathParts: route.path.replace(/^\/+|\/+$/g, '').split('/'),
    clientDisplayPathParts: clientDisplayPath.clientPathInfo.path?.replace(/^\/+|\/+$/g, '')?.split('/'),
    hasSkillsClientDisplayPath: clientDisplayPath.clientPathInfo && clientDisplayPath.clientPathInfo.path
  }
}
let pathInfo = buildPathInfo()

const buildBreadcrumb = () => {
  pathInfo = buildPathInfo()

  const newItems = []
  let res = pathInfo.pathParts
  if (pathInfo.hasSkillsClientDisplayPath) {
    res = [...res, ...pathInfo.clientDisplayPathParts]
  }
  let key = null

  if (typeof route.meta.breadcrumb === 'function') {
    items.value = route.meta.breadcrumb(route)
    return
  }

  const lastItemInPathCustomName = route.meta.breadcrumb

  let ignoreNext = false
  res.forEach((item, index) => {
    let value = item === adminHomePageItem ? adminHomePageName : item
    if (value) {
      if (!ignoreNext && item !== 'global') {
        // treat crossProject as a special case
        if (value === 'crossProject') {
          ignoreNext = true
          key = 'Prerequisite'
          return
        }
        // treat userTag as a special case
        if (value === 'userTag') {
          ignoreNext = true
          key = res[index + 1]
          return
        }
        if (value === projectAndRankingPathItem && !appConfig.rankingAndProgressViewsEnabled) {
          return
        }
        if (index === pathInfo.pathParts.length - 1 && lastItemInPathCustomName) {
          key = null
          value = lastItemInPathCustomName
        }

        if (key) {
          if (!shouldExcludeKey(key)) {
            if (key?.toLowerCase() === 'skills' && value?.toLowerCase() === 'inception') {
              newItems.push(buildResItem(null, 'Dashboard Skills', res, index))
            } else {
              newItems.push(buildResItem(key, value, res, index))
            }
          }
          key = null
        } else {
          // must exclude items in the path because each page with navigation
          // doesn't have a sub-route in the url, for example:
          // '/projects/projectId' will conceptually map to '/projects/projectId/subjects'
          // but there is no '/project/projectId/subjects' route configured so when parsing something like
          // '/projects/projectId/subjects/subjectId/stats we must end up with:
          //    'projects / project:projectId / subject:subjectId / stats'
          // notice that 'subjects' is missing
          if (!shouldExcludeValue(value) && !isQuizzesValueUnderProgressAndRanking(value, res)) {
            newItems.push(buildResItem(key, value, res, index))
          }
          if (value !== 'Projects' && value !== projectAndRankingPathItem && value !== lastItemInPathCustomName) {
            key = value
          }
        }
      } else {
        ignoreNext = false
      }
    }
  })

  if (newItems.length > 0) {
    newItems[0].isFirst = true
    newItems[newItems.length-1].isLast = true
  }
  items.value = newItems
}
const buildResItem = (key, item, res, index) => {
  const decodedItem = decodeURIComponent(item)
  return {
    icon: index === 0 ? getIcon(decodedItem) : null,
    label: key ? prepKey(key) : null,
    value: !key ? capitalize(hyphenToCamelCase(decodedItem)) : decodedItem,
    url: getUrl(res, index + 1),
    isLast: false,
  }
}
const getIcon = (value) => {
  let icon = null
  if (value === adminHomePageName){
    icon = 'fas fa-tasks'
  }
  if (value === settingsHomePageItem) {
    icon = 'fas fa-wrench'
  }
  if (value === projectAndRankingPathItem) {
    icon = 'fas fa-chart-bar'
  }

  return icon
}
const getUrl = (arr, endIndex) => {
  const dashboardUrlSize = pathInfo.pathParts.length
  let url = `/${arr.slice(0, Math.min(endIndex, dashboardUrlSize)).join('/')}/`
  if (pathInfo.hasSkillsClientDisplayPath && endIndex >= dashboardUrlSize) {
    const skillsClientDisplayPath = (endIndex > dashboardUrlSize) ? `/${arr.slice(dashboardUrlSize, endIndex).join('/')}` : '/'
    const queryParams = new URLSearchParams(window.location.search)
    queryParams.set('skillsClientDisplayPath', skillsClientDisplayPath)
    url += `?${queryParams.toString()}`
  }
  return url
}
const prepKey = (key) => {
  let res = key
  if (key.endsWith('zes')) {
    res = key.substring(0, key.length - 3)
  } else {
    res = key.endsWith('s') ? key.substring(0, key.length - 1) : key
  }

  return capitalize(substituteCustomLabels(res))
}
const substituteCustomLabels = (label) => {
  if (label.toLowerCase() === 'project') {
    return projectDisplayName.value
  }
  if (label.toLowerCase() === 'subject') {
    return subjectDisplayName.value
  }
  if (label.toLowerCase() === 'group') {
    return groupDisplayName.value
  }
  if (label.toLowerCase() === 'skill') {
    return skillDisplayName.value
  }
  return label
}
const hyphenToCamelCase = (value) => {
  return value.replace(/-([a-z])/g, (g) => ` ${g[1].toUpperCase()}`)
}
const capitalize = (value) => {
  return value.charAt(0).toUpperCase() + value.slice(1)
}
const shouldExcludeValue = (item) => {
  return idsToExcludeFromPath.some((searchForMe) => item === searchForMe)
}
const shouldExcludeKey = (key) => {
  return keysToExcludeFromPath.some((searchForMe) => key === searchForMe)
}
const isQuizzesValueUnderProgressAndRanking = (value, items) => {
  const isQuizzes = value === 'quizzes'
  return isQuizzes && items.includes('progress-and-rankings')
}


</script>

<template>
  <Card :pt="{ body: { class: 'p-0!' } }" class="mx-3">
    <template #content>
      <Breadcrumb :model="items"
                :class="{'dashboard-breadcrumb-vertical-mode': smallScreenMode}"
                aria-label="SkillTree Dashboard Breadcrumb Bar"
                data-cy="breadcrumb-bar">
      <template #item="{ item, props }">
        <router-link
          v-if="!item.isLast"
          v-slot="{ href, navigate }"
          :to="item.url"
          custom>
          <a :href="href" v-bind="props.action" @click="navigate" :data-cy="`breadcrumb-${item.value}`">
            <skills-breadcrumb-item
              :icon="item.icon"
              :label="item.label"
              :value="item.value"
              :show-separator="!item.isFirst && smallScreenMode"
              value-css="text-primary" />
          </a>
        </router-link>
        <div v-else :data-cy="`breadcrumb-${item.value}`">
          <skills-breadcrumb-item
            :icon="item.icon"
            :label="item.label"
            :show-separator="item.value && smallScreenMode"
            :value="item.value" />
        </div>
      </template>
    </Breadcrumb>
    </template>
  </Card>
</template>
<style>
.dashboard-breadcrumb-vertical-mode ol {
  display: block
}
.dashboard-breadcrumb-vertical-mode .p-menuitem-separator {
  display: none
}
</style>
<style scoped></style>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useRoute } from 'vue-router'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import SkillsBreadcrumbItem from '@/components/header/SkillsBreadcrumbItem.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayBreadcrumbState } from '@/skills-display/stores/UseSkillsDisplayBreadcrumbState.js'

const themeState = useSkillsDisplayThemeState()
const displayPreferences = useSkillsDisplayPreferencesState()
const skillsDisplayInfo = useSkillsDisplayInfo()
const route = useRoute()
const breadcrumbState = useSkillsDisplayBreadcrumbState()
const disabled = computed(() => themeState.theme.disableBreadcrumb)

onMounted(() => {
  build()
})

const build = () => {
  let ignoreNext = false
  const path = skillsDisplayInfo.cleanPath(route.path)
  const res = path.split('/').filter((item) => item)
  res.unshift('')
  let key = null
  const newItems = []
  if (res) {
    res.forEach((item, index) => {
      if (!ignoreNext && item !== 'global') {
        const value = item === '' ? 'Overview' : item
        // treat crossProject as a special case
        if (value === 'crossProject') {
          ignoreNext = true
          key = 'Dependency'
          return
        }
        if (key) {
          if (!shouldExcludeKey(key)) {
            newItems.push(buildResItem(key, value, res, index))
          }
          key = null
        } else {
          // must exclude items in the path because each page with navigation
          // when parsing something like '/subjects/subj1/skills/skill1' we must end up with:
          // 'Overview / subjects:subj1/ skills:skill1'
          if (!shouldExcludeValue(value)) {
            newItems.push(buildResItem(key, value, res, index))
          }
          if (value !== 'Overview') {
            key = value
          }
        }
      } else {
        ignoreNext = false
      }
    })
  }
  if (newItems.length > 0) {
    newItems[newItems.length - 1].isLast = true
  }
  breadcrumbState.breadcrumbItems = newItems
}

const idsToExcludeFromPath = ['subjects', 'skills', 'crossProject', 'dependency', 'global', 'quizzes']
const keysToExcludeFromPath = []
const shouldExcludeKey = (key) => {
  keysToExcludeFromPath.some((searchForMe) => key === searchForMe)
}
const shouldExcludeValue = (item) => {
  return idsToExcludeFromPath.some((searchForMe) => item === searchForMe)
}

const buildResItem = (key, item, res, index) => {
  const decodedItem = decodeURIComponent(item)
  const url = getUrl(res, index + 1)
  return {
    label: key ? prepKey(key) : null,
    value: !key ? capitalize(hyphenToCamelCase(decodedItem)) : decodedItem,
    url,
    contextUrl: getContextUrl(url)
  }
}
const getUrl = (arr, endIndex) => {
  const prefix = endIndex === 1 ? '/' : ''
  return `${prefix}${arr.slice(0, endIndex).join('/')}`
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

const capitalize = (value) => {
  return value.charAt(0).toUpperCase() + value.slice(1)
}
const hyphenToCamelCase = (value) => {
  return value.replace(/-([a-z])/g, (g) => ` ${g[1].toUpperCase()}`)
}
const substituteCustomLabels = (label) => {
  if (label.toLowerCase() === 'project') {
    return displayPreferences.projectDisplayName
  }
  if (label.toLowerCase() === 'subject') {
    return displayPreferences.subjectDisplayName
  }
  if (label.toLowerCase() === 'group') {
    return displayPreferences.groupDisplayName
  }
  if (label.toLowerCase() === 'skill') {
    return displayPreferences.skillDisplayName
  }
  return label
}
const getContextUrl = (url) => `${skillsDisplayInfo.getRootUrl()}${url}`


</script>

<template>
  <div v-if="!disabled"
       class="skills-theme-breadcrumb-container flex justify-content-center" data-cy="skillsDisplayBreadcrumbBar">
    <Breadcrumb :model="breadcrumbState.breadcrumbItems" :pt="{ root: { class: 'border-none px-0 py-1' } }">
      <template #item="{ item, props }">
        <router-link
          v-if="!item.isLast"
          v-slot="{ href, navigate }"
          :to="getContextUrl(item.url)"
          custom>
          <a :href="href" v-bind="props.action" @click="navigate" :data-cy="`breadcrumbLink-${item.value}`">
            <skills-breadcrumb-item
              :icon="item.icon"
              :label="item.label"
              :value="item.value"
              value-css="text-primary" />
          </a>
        </router-link>
        <div v-else>
          <skills-breadcrumb-item
            :icon="item.icon"
            :label="item.label"
            :value="item.value" />
        </div>
      </template>
    </Breadcrumb>
  </div>
</template>

<style scoped>

</style>
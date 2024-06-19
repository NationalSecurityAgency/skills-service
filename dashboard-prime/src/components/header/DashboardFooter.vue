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
import SkillTreeArrows from '@/components/header/SkillTreeArrows.vue'
import dayjs from 'dayjs'
import { computed } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const appConfig = useAppConfig()

const skillTreeVersionTitle = computed(() => {
    const dateString = dayjs(appConfig.artifactBuildTimestamp).format('llll [(]Z[ from UTC)]')
    return `Build Date: ${dateString}`
  }
)
const supportLinksProps = computed(() => {
  // const configs = this.$store.getters.config
  // const dupKeys = appConfig.getConfigsThatStartsWith('supportLink')
  //   .map((filteredConf) => filteredConf.substr(0, 12))
  const configs = appConfig.getConfigsThatStartsWith('supportLink')
  const dupKeys = Object.keys(configs).map((conf) => conf.substring(0, 12))
  //Object.keys(configs).filter((conf) => conf.startsWith('supportLink')).map((filteredConf) => filteredConf.substr(0, 12))
  const keys = dupKeys.filter((v, i, a) => a.indexOf(v) === i)
  return keys.map((key) => ({
    link: configs[key],
    label: configs[`${key}Label`],
    icon: configs[`${key}Icon`]
  }))
})


</script>

<template>
  <div class="mt-4 py-4 pr-3 pl-2 flex bg-primary-reverse border-top border-top-1 border-200"
       data-cy="dashboardFooter">
    <div class="flex-1 flex align-items-end align-content-end">
      <skill-tree-arrows />
      <div>
        <div>
          SkillTree Dashboard
        </div>
        <div v-if="supportLinksProps && supportLinksProps.length > 0">
              <span v-for="(supportLink, index) in supportLinksProps" :key="supportLink.label">
                <a :href="supportLink.link" class="" :data-cy="`supportLink-${supportLink.label}`"
                   target="_blank"><u><i :class="supportLink.icon" class="mr-1" />{{ supportLink.label }}</u></a>
                <span v-if="index < supportLinksProps.length - 1" class="mx-1">|</span>
              </span>
        </div>
      </div>
    </div>
    <div class="flex align-items-end" data-cy="dashboardVersionContainer">
      <div class="mr-2"
            :title="skillTreeVersionTitle"
            data-cy="dashboardVersion">v{{ appConfig.dashboardVersion }}</div>
      <i class="fas fa-code-branch"></i>
    </div>
  </div>
</template>

<style scoped>

</style>
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
import {computed} from 'vue'
import {useAppConfig} from '@/common-components/stores/UseAppConfig.js'
import {useSupportLinksUtil} from "@/components/contact/UseSupportLinksUtil.js";

const appConfig = useAppConfig()
const supportLinksUtil = useSupportLinksUtil()

const skillTreeVersionTitle = computed(() => {
    const dateString = dayjs(appConfig.artifactBuildTimestamp).format('llll [(]Z[ from UTC)]')
    return `Build Date: ${dateString}`
  }
)
const supportLinksProps = supportLinksUtil.supportLinks

</script>

<template>
  <div class="mt-6 py-6 px-4 text-gray-600 dark:text-gray-100 flex bg-primary-contrast border-top border-t border-surface-200 dark:border-surface-600 flex-col sm:flex-row gap-4 sm:gap-0"
       data-cy="dashboardFooter">
    <div class="flex-1 flex sm:justify-start items-end content-end text-left">
      <skill-tree-arrows />
      <div>
        <div class="text-primary">
          SkillTree Dashboard
        </div>
        <div v-if="supportLinksProps && supportLinksProps.length > 0" class="">
              <span v-for="(supportLink, index) in supportLinksProps" :key="supportLink.label">
                <a :href="supportLink.url" class="underline cursor-pointer" :data-cy="`supportLink-${supportLink.label}`"
                   @click="supportLink.command"
                   target="_blank"><i :class="supportLink.icon" class="mr-1" aria-hidden="true"/>{{ supportLink.label}}</a>
                <span v-if="index < supportLinksProps.length - 1" class="mx-1">|</span>
              </span>
        </div>
      </div>
    </div>
    <div class="flex items-end ml-4 sm:ml-0" data-cy="dashboardVersionContainer">
      <div class="mr-2"
            :title="skillTreeVersionTitle"
            data-cy="dashboardVersion">v{{ appConfig.dashboardVersion }}</div>
      <i class="fas fa-code-branch"></i>
    </div>
  </div>
</template>

<style scoped>

</style>
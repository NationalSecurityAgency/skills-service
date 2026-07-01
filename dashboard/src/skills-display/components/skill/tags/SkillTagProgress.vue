/*
Copyright 2026 SkillTree

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
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'

const props = defineProps({
  skillTagOverview: Object,
  index: {
    type: Number,
    default: -1
  },
  buildLink: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const attributes = useSkillsDisplayAttributesState()
const skillsDisplayInfo = useSkillsDisplayInfo()
const colors = useColors()
const nF = useNumberFormat()

const tagName = computed(() => {
  return props.skillTagOverview.tag || props.skillTagOverview.tagValue || route.query.tagValue || route.params.tagId
})

const skillsAchieved = computed(() => props.skillTagOverview.skillsAchieved || 0)
const totalSkills = computed(() => props.skillTagOverview.totalSkills || props.skillTagOverview.skills?.length || 0)

const progressPercent = computed(() => {
  return totalSkills.value > 0 ? Math.trunc((skillsAchieved.value / totalSkills.value) * 100) : 0
})
const iconClass = computed(() => {
  const color = props.index >= 0 ? colors.getTextClass(props.index): ''
  return `fa-solid fa-tag text-4xl ${color}`
})
</script>

<template>
  <div>
    <Card>
      <template #content>
        <div class="flex flex-col gap-4">
          <div class="flex items-center gap-3">
            <div class="border rounded p-3 text-primary">
              <i :class="iconClass" aria-hidden="true"></i>
            </div>

            <div class="flex-1 flex flex-col gap-2">
              <div class="flex items-end gap-1">
                <div class="flex-1">

                  <router-link
                      v-if="buildLink"
                      :to="{ name: skillsDisplayInfo.getContextSpecificRouteName('skillTagDetails'), params: { tagId: skillTagOverview.tagId } }"
                      :data-cy="`tagLink-${skillTagOverview.tagId}`">{{ tagName }}</router-link>
                  <span v-else class="text-3xl" data-cy="skillTagName">{{ tagName }}</span>
                </div>

                <div data-cy="skillTagProgress">
                  <span>{{ nF.pretty(skillsAchieved) }}</span>
                  /
                  <span>{{ nF.pretty(totalSkills) }}</span>
                  {{ attributes.skillDisplayNamePlural }}
                </div>
              </div>

              <div>
                <vertical-progress-bar
                  :total-progress="progressPercent"
                  :disable-daily-color="true" />
              </div>
            </div>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
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
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";
import SkillsTitle from "@/skills-display/components/utilities/SkillsTitle.vue";
import {useSkillsDisplaySubjectState} from "@/skills-display/stores/UseSkillsDisplaySubjectState.js";
import {computed, onMounted, ref} from "vue";
import {useRoute} from "vue-router";
import SkillsProgressList from "@/skills-display/components/progress/SkillsProgressList.vue";
import VerticalProgressBar from "@/skills-display/components/progress/VerticalProgressBar.vue";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import SkillType from "@/common-components/utilities/SkillType.js";

const route = useRoute()
const attributes = useSkillsDisplayAttributesState()
const summaryAndSkillsState = useSkillsDisplaySubjectState()
const nF = useNumberFormat()

const group = computed(() => summaryAndSkillsState.subjectSummary)
const isLoading = computed(() => summaryAndSkillsState.loadingSkillsGroupSummary)

onMounted(() => {
  loadGroupSummary()
})

const loadGroupSummary = () => {
  summaryAndSkillsState.loadSkillsGroupSummary(route.params.groupId)
}
const progressPercent = computed(() => group.value.totalSkills > 0 ? Math.trunc((group.value.skillsAchieved / group.value.totalSkills) * 100)  : 0)
</script>

<template>
  <div>
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-8"/>
    <div v-if="!isLoading" class="flex flex-col gap-3">
      <div>
        <skills-title>{{ attributes.groupDisplayName }} Overview</skills-title>
      </div>

      <Card>
        <template #content>
          <div class="flex flex-col gap-4">
            <div class="flex items-center gap-3">
              <div class="border rounded p-3 text-primary">
                  <i class="fa-solid fa-layer-group text-4xl" aria-hidden="true"></i>
              </div>
              <div class="flex-1 flex flex-col gap-2">
                <div class="flex items-end gap-1">
                  <div class="flex-1">
                    <span class="text-3xl" data-cy="skillsGroupName">{{ group.group}}</span>
                  </div>
                  <div data-cy="skillsGroupProgress">
                    <span>{{ nF.pretty(group.skillsAchieved) }}</span> / <span>{{ nF.pretty(group.totalSkills) }}</span> {{ attributes.skillDisplayNamePlural }}
                  </div>
                </div>
                <div>
                  <vertical-progress-bar :total-progress="progressPercent" :disable-daily-color="true"/>
                </div>
              </div>
            </div>
            <div v-if="group.description">
              <markdown-text :text="group.description" data-cy="skillsGroupDescription"/>
            </div>
          </div>
        </template>
      </Card>

      <skills-progress-list :type="SkillType.SkillsGroup"/>
    </div>
  </div>
</template>

<style scoped>

</style>
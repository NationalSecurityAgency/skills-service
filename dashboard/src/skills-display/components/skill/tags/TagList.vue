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

import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const attributes = useSkillsDisplayAttributesState()
const skillsDisplayService = useSkillsDisplayService()
const route = useRoute()
const colors = useColors()
const getBgColor = (index) => `${colors.getBgColorClass(index, 50)} dark:bg-slate-800`
const tags = ref([])

onMounted(() => {
  loadData()
})
const loadData = async () => {
  const projectId = attributes.projectId
  const { subjectId } = route.params
  tags.value = subjectId
    ? await skillsDisplayService.getTagsForSubject(projectId, subjectId)
    : await skillsDisplayService.getTagsForProject(projectId)
}
const hasTags = computed(() => tags.value?.length > 0)
</script>

<template>
  <div class="">
    <Card v-if="hasTags" class="mb-3">
      <template #content>
        <h2 class="sr-only">Skill Tags</h2>
        <div data-cy="skillTags" class="flex gap-2">
          <div v-for="(tag, index) in tags"
               class="border rounded-xl px-2 py-1 text-slate-900 dark:text-slate-100 dark:bg-slate-800 dark:border-slate-600"
               :class="getBgColor(index)"
               icon="fa-solid fa-tag"
               severity="secondary">
            <div class="flex gap-1 items-center">
              <i class="fa-solid fa-tag" aria-hidden="true" :class="colors.getTextClass(index)"></i>
              <div>{{ tag.tagValue }}</div>
              <div class="border rounded-3xl px-2 bg-gray-200 text-slate-900 dark:bg-slate-700 dark:text-slate-100 dark:border-slate-500">
                {{ tag.numSkills }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>